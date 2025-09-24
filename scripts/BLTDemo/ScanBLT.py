import asyncio
from bleak import BleakScanner
import matplotlib.pyplot as plt
from collections import deque
import numpy as np

# === Configuration ===
# Mapping beacon names to logical room labels
BEACONS = {
    "BCPro_207277": "Bedroom1",
    "BCPro_207328": "Kitchen",
    "BCPro_207539": "Bedroom2"
}

# TX power at 1 meter (adjust based on your beacon specs)
TX_POWER = -59
# Path-loss exponent (environment factor, usually 2.0 ~ 3.0)
N = 2.0

# RSSI smoothing window (for moving average)
WINDOW_SIZE = 5
RSSI_HISTORY = {name: deque(maxlen=WINDOW_SIZE) for name in BEACONS}

# Current area tracking
current_area = None
current_distance = None
# Threshold margin (in meters) to avoid frequent area switching
SWITCH_MARGIN = 0.5

def rssi_to_distance(rssi: int) -> float:
    """Convert RSSI value to estimated distance using log-distance path loss model."""
    return 10 ** ((TX_POWER - rssi) / (10 * N))

# === Visualization: simple floor plan ===
fig, ax = plt.subplots(figsize=(8, 4))
ax.set_xlim(0, 3)
ax.set_ylim(0, 1)
ax.axis("off")

# Room rectangles
rects = {
    "Bedroom1": plt.Rectangle((0, 0), 1, 1, color="lightgray"),
    "Kitchen": plt.Rectangle((1, 0), 1, 1, color="lightgray"),
    "Bedroom2": plt.Rectangle((2, 0), 1, 1, color="lightgray")
}
for r in rects.values():
    ax.add_patch(r)

# Room labels
ax.text(0.5, 0.5, "Bedroom1", ha="center", va="center", fontsize=14)
ax.text(1.5, 0.5, "Kitchen", ha="center", va="center", fontsize=14)
ax.text(2.5, 0.5, "Bedroom2", ha="center", va="center", fontsize=14)

plt.ion()
plt.show()

def highlight_area(area_name):
    """Highlight the currently detected area in green."""
    for name, rect in rects.items():
        rect.set_color("lime" if name == area_name else "lightgray")
    fig.canvas.draw()
    fig.canvas.flush_events()

def detection_callback(device, advertisement_data):
    """Callback function triggered when a BLE advertisement is detected."""
    global current_area, current_distance
    if device.name in BEACONS:
        # Collect and smooth RSSI using moving average
        RSSI_HISTORY[device.name].append(advertisement_data.rssi)
        avg_rssi = np.mean(RSSI_HISTORY[device.name])
        distance = rssi_to_distance(avg_rssi)
        area = BEACONS[device.name]

        # Initial assignment
        if current_area is None:
            current_area, current_distance = area, distance
            highlight_area(area)
            print(f"📡 Initial: {area}, RSSI={avg_rssi:.1f} dBm ≈ {distance:.2f}m")
            return

        # Switch logic: only change area if the new one is significantly closer
        if area != current_area and distance < (current_distance - SWITCH_MARGIN):
            current_area, current_distance = area, distance
            highlight_area(area)
            print(f"➡️ Switch to {area}, RSSI={avg_rssi:.1f} dBm ≈ {distance:.2f}m")
        elif area == current_area:
            # Update distance if still in the same area
            current_distance = distance

async def main():
    """Main entry point: start BLE scanning and handle events."""
    print("🔎 Scanning for beacons... (Ctrl+C to stop)")
    scanner = BleakScanner(detection_callback, scanning_mode="active")  # Active mode = more frequent scan results
    await scanner.start()
    try:
        while True:
            await asyncio.sleep(1)  # Keep loop alive
    finally:
        await scanner.stop()
        print("✅ Scan stopped.")

if __name__ == "__main__":
    asyncio.run(main())
