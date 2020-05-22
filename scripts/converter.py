import sys

lines = [l.strip().split(",") for l in open(sys.argv[1], "r").readlines()]

old_header = lines[0]
new_header = "timestamp,x_acc,y_acc,z_acc,x_ang_vel,y_ang_vel,z_ang_vel,latitude,longitude,speed".split(",")

mapping = {
    "timestamp": "timestamp",
    "x_acc": "x_med_acc",
    "y_acc": "y_med_acc",
    "z_acc": "z_med_acc",
    "x_ang_vel": "x_med_ang_vel",
    "y_ang_vel": "y_med_ang_vel",
    "z_ang_vel": "z_med_ang_vel",
    "latitude": "latitude",
    "longitude": "longitude",
    "speed": "speed"
}

out = open(sys.argv[1] + ".stripped", "w")
out.write(",".join(new_header) + "\n")

for line in lines[1:]:
    out.write(",".join([line[old_header.index(mapping[column])] for column in new_header]) + "\n")

out.close()