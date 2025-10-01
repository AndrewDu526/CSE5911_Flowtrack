# Compiler and flags
CXX = g++
CXXFLAGS = -std=c++11 -O3 -Wall -Wextra -fopenmp

# Target executable
TARGET = clustering

# Source and object files
SOURCES = clustering.cpp
OBJECTS = $(SOURCES:.cpp=.o)

# Header files
HEADERS = clustering.h\
		  .\..\projutils.h

# Default target
all: $(TARGET)

# Link object files to create executable
$(TARGET): $(OBJECTS)
	$(CXX) $(OBJECTS) -o $(TARGET) $(CXXFLAGS)

# Compile source files to object files
%.o: %.cpp $(HEADERS)
	$(CXX) $(CXXFLAGS) -c $< -o $@

# TODO - make this cross-platform with windows
clean_linux:
	rm -f $(OBJECTS) $(TARGET)

clean_win:
	CLEAN_CMD = del /F /Q $(OBJECTS) $(TARGET)

# Phony targets
.PHONY: all clean
