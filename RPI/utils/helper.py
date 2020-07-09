import datetime
import json

class StringHelper():

    def __init__(self):
        pass

    @staticmethod
    def extractFromPassedDict(d: dict):
        for k, v in d.items():
            json_string = k
        r = json.loads(json_string)
        for k, v in r.items():
            return (k, v)
        
    @staticmethod
    def timestamp2Readable(timestamp):
        if isinstance(timestamp, str):
            timestamp = float(timestamp)
        return datetime.datetime.utcfromtimestamp(timestamp).strftime('%Y-%m-%dT%H:%M:%SZ')