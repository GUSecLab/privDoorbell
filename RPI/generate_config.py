import os
import json


if (not os.path.isfile("seed.conf")) or (not os.path.isfile("hostname.conf")):
    raise FileExistsError("Conf files don't exist!")


with open("seed.conf") as f:
    seed = f.read().strip()

with open("hostname.conf") as f:
    hostname = f.read().strip()


d = {
    "seed": seed,
    "onion_hostname": hostname,
    "detector_file": None,
    "detector_scaleFactor": 1.3,
    "detector_minNeighbors": 3,
    "detector_minHeight": 55,
    "detector_minWidth": 55,
    "detector_maxHeight": None,
    "detector_maxWidth": None,
    "video_input": "/dev/video0",
    "audio_input": None,
}

with open("config.json") as f:
    json.dump(d, f)