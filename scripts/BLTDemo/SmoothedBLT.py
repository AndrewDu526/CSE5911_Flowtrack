import asyncio
from bleak import BleakScanner
import matplotlib.pyplot as plt
from collections import deque
import numpy as np

# === Configuration ===
BEACONS = {
    "BCPro_207277": "Bedroom1",
    "BCPro_207328": "Kitchen",
    "BCPro_207539": "Bedroom2"
}
TX_POWER = -59   # Reference RSSI at 1 meter
N = 2.0          # Path-loss exponent (environment dependent)

# Sliding average window for RSSI smoothing
WINDOW_SIZE = 5
RSSI_HISTORY = {name: deque(maxlen=WINDOW_SIZE) for name in BEACONS}
RAW_HISTORY = {name: deque(maxlen=100) for name in BEACONS}
SMOOTH_HISTORY = {name: deque(maxlen=100) for name in BEACONS}
TIME_AXIS = deque(maxlen=100)
t_counter = 0

# Area state
current_area = None
current_distance = None
SWITCH_MARGIN = 0.5   # Hysteresis margin (meters) for switching between areas

# === Matplotlib Setup ===
plt.ion()
fig, (ax_area, ax_rssi) = plt.subplots(2, 1, figsize=(10, 8))

# Top plot: area layout
ax_area.set_xlim(0, 3)
ax_area.set_ylim(0, 1)
ax_area.axis("off")

rects = {
    "Bedroom1": plt.Rectangle((0, 0), 1, 1, color="lightgray"),
    "Kitchen": plt.Rectangle((1, 0), 1, 1, color="lightgray"),
    "Bedroom2": plt.Rectangle((2, 0), 1, 1, color="lightgray")
}
for r in rects.values():
    ax_area.add_patch(r)

ax_area.text(0.5, 0.5, "Bedroom1", ha="center", va="center", fontsize=14)
ax_area.text(1.5, 0.5, "Kitchen", ha="center", va="center", fontsize=14)
ax_area.text(2.5, 0.5, "Bedroom2", ha="center", va="center", fontsize=14)

# Bottom plot: RSSI curves
ax_rssi.set_title("Beacon RSSI (Raw vs Smoothed)")
ax_rssi.set_xlabel("Time (samples)")
ax_rssi.set_ylabel("RSSI (dBm)")
ax_rssi.set_ylim(-100, -30)

# Two lines per beacon: Raw + Smoothed
lines_raw = {name: ax_rssi.plot([], [], "--", alpha=0.5, label=f"{BEACONS[name]} Raw")[0] for name in BEACONS}
lines_smooth = {name: ax_rssi.plot([], [], label=f"{BEACONS[name]} Smoothed")[0] for name in BEACONS}
ax_rssi.legend()


def rssi_to_distance(rssi: int) -> float:
    """Convert RSSI to estimated distance using log-distance path loss model."""
    return 10 ** ((TX_POWER - rssi) / (10 * N))


def highlight_area(area_name):
    """Highlight the currently detected area."""
    for name, rect in rects.items():
        rect.set_color("lime" if name == area_name else "lightgray")


def update_plot():
    """Refresh both the area and RSSI plots."""
    global t_counter
    TIME_AXIS.append(t_counter)
    t_counter += 1

    for dev in BEACONS:
        # Update raw RSSI line
        if RAW_HISTORY[dev]:
            lines_raw[dev].set_data(range(len(RAW_HISTORY[dev])), RAW_HISTORY[dev])
        # Update smoothed RSSI line
        if SMOOTH_HISTORY[dev]:
            lines_smooth[dev].set_data(range(len(SMOOTH_HISTORY[dev])), SMOOTH_HISTORY[dev])

    # Adjust x-axis to keep the plot scrolling
    ax_rssi.set_xlim(0, max(50, len(TIME_AXIS)))

    fig.canvas.draw_idle()
    fig.canvas.flush_events()


def detection_callback(device, advertisement_data):
    """Callback triggered when BLE advertisements are received."""
    global current_area, current_distance
    if device.name in BEACONS:
        # Store raw RSSI
        RAW_HISTORY[device.name].append(advertisement_data.rssi)

        # Smooth RSSI using moving average
        RSSI_HISTORY[device.name].append(advertisement_data.rssi)
        avg_rssi = np.mean(RSSI_HISTORY[device.name])
        SMOOTH_HISTORY[device.name].append(avg_rssi)

        distance = rssi_to_distance(avg_rssi)
        area = BEACONS[device.name]

        # Initial detection
        if current_area is None:
            current_area, current_distance = area, distance
            highlight_area(area)
            print(f"📡 Initial: {area}, RSSI={avg_rssi:.1f} dBm ≈ {distance:.2f}m")
            return

        # Switching logic with hysteresis
        if area != current_area and distance < (current_distance - SWITCH_MARGIN):
            current_area, current_distance = area, distance
            highlight_area(area)
            print(f"➡️ Switch to {area}, RSSI={avg_rssi:.1f} dBm ≈ {distance:.2f}m")
        elif area == current_area:
            current_distance = distance  # Update distance for the current area


async def main():
    """Main loop: start scanning and update plots in real time."""
    print("🔎 Scanning for beacons... (Ctrl+C to stop)")
    scanner = BleakScanner(detection_callback, scanning_mode="active")
    await scanner.start()
    try:
        while True:
            update_plot()
            await asyncio.sleep(0.5)
    finally:
        await scanner.stop()
        print("✅ Scan stopped.")


if __name__ == "__main__":
    plt.show(block=False)
    asyncio.run(main())
