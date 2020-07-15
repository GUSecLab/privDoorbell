import datetime
import json

class StringHelper():

    def __init__(self):
        pass

    @staticmethod
    def extractFromPassedDict(d: dict):
        '''
        Return (firebase_token, nickname, device_token)
        '''
        for k, v in d.items():
            json_string = k
        r = json.loads(json_string)
        if (not 'firebase_token' in r) or (not 'nickname' in r) or (not 'device_token' in r):
            raise Exception("Bad response")
        return r['firebase_token'], r['nickname'], r['device_token']
        
    @staticmethod
    def timestamp2Readable(timestamp):
        if isinstance(timestamp, str):
            timestamp = float(timestamp)
        return datetime.datetime.utcfromtimestamp(timestamp).strftime('%Y-%m-%dT%H:%M:%SZ')