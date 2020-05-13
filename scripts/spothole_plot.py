#!/usr/bin/env python3

import pandas as pd
from matplotlib import pyplot as plt
from matplotlib.widgets import Slider
import matplotlib.patches as mpatches
import sys


def read_csv(file_name):
    return pd.read_csv(file_name, sep=",", header=0)

class ScrollablePlot:
    def __init__(self, title, window_elements=250, vertical_extra_space=0.05):
        self.values = []
        self.colors = []
        self.labels = []
        self.window_elements = window_elements
        self.y_boundaries = []
        self.vertical_extra_space = vertical_extra_space
        self.title = title
        self.events_colors = {
            "Buca": "red",
            "Giunto": "blue",
            "Tombino": "green",
            "Dosso": "orange"
        }

    def set_timestamps(self, timestamps, relative=True):
        self.initial_timestamp = timestamps[0]
        if relative:
            self.timestamps = timestamps - self.initial_timestamp
        else:
            self.timestamps = timestamps

        self.window_width = self.timestamps[min(self.window_elements, len(timestamps)-1)] - self.timestamps[0]
        
    def add_axe(self, values, color="black", label=""):
        self.values.append(values)
        self.colors.append(color)
        self.labels.append(label)

        min_y = min(values)
        max_y = max(values)
        extra = (max_y - min_y) * self.vertical_extra_space
        self.y_boundaries.append([min_y - extra, max_y + extra])
    
    def add_events(self, timestamps, types):
        self.events_timestamps = timestamps - self.initial_timestamp
        self.events_types = types
    
    def show(self):
        fig, axes = plt.subplots(len(self.values))
        fig.suptitle(self.title, fontsize=16)
        if len(self.values) == 1:
            axes = [axes]
            
        plt.subplots_adjust(top=0.95, bottom=0.25, left=0.1, right=0.95)

        slider_pos = plt.axes([0.1, 0.05, 0.85, 0.02])
        slider_obj = Slider(slider_pos, 'Pos', self.timestamps[0], self.timestamps[len(self.timestamps)-1] - self.window_width)

        def update_positions(val):
            position = slider_obj.val
            for i, axe in enumerate(axes):
                y_boundaries = self.y_boundaries[i]
                axe.axis([position, position+self.window_width, y_boundaries[0], y_boundaries[1]])
            fig.canvas.draw_idle()
        

        for i, axe in enumerate(axes):
            y_boundaries = self.y_boundaries[i]
            axe.axis([0, self.window_width, y_boundaries[0], y_boundaries[1]])
            axe.plot(self.timestamps, self.values[i], "r.-", markersize=4, linewidth=1, color=self.colors[i])
            axe.set_ylabel(self.labels[i])
            if i < len(axes) - 1:
                axe.get_xaxis().set_visible(False)

            for event_timestamp, event_type in zip(self.events_timestamps, self.events_types):
                axe.axvline(x=event_timestamp, ymin=0, ymax=1, color=self.events_colors[event_type], label=event_type)
        
        plt.legend(handles=[mpatches.Patch(color=color, label=event) for event, color in self.events_colors.items()])

        slider_obj.on_changed(update_positions)
        plt.show()


def main():
    if len(sys.argv) == 3:
        values_filename = sys.argv[1]
        events_filename = sys.argv[2]
    else:
        values_filename = "sensors.csv"
        events_filename = "obstacles.csv"

    values = read_csv(values_filename) # timestamp,x_acc,y_acc,z_acc,x_ang_vel,y_ang_vel,z_ang_vel,latitude,longitude,speed
    clicks = read_csv(events_filename)

    timestamps = values.loc[:, "timestamp"]

    scrollable_plot = ScrollablePlot(title="Spothole data")
    scrollable_plot.set_timestamps(timestamps)    
    scrollable_plot.add_axe(values.loc[:, "x_acc"], "red", "x_acc")
    scrollable_plot.add_axe(values.loc[:, "y_acc"], "green", "y_acc")
    scrollable_plot.add_axe(values.loc[:, "z_acc"], "blue", "z_acc")
    scrollable_plot.add_axe(values.loc[:, "x_ang_vel"], "red", "x_giro")
    scrollable_plot.add_axe(values.loc[:, "y_ang_vel"], "green", "y_giro")
    scrollable_plot.add_axe(values.loc[:, "z_ang_vel"], "blue", "z_giro")
    scrollable_plot.add_events(clicks.loc[:, "timestamp"], clicks.loc[:, "type"])

    scrollable_plot.show()

main()
