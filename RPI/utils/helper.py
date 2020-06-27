import json

class StringHelper():

    def __init__(self):
        pass

    @staticmethod
    def extractFromPassedDict(d: dict) -> tuple[str]:
        for k, v in d.items():
            json_string = k
        r = json.loads(json_string)
        for k, v in r.items():
            return (k, v)
        