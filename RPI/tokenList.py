import time
import datetime
import json
from utils.helper import StringHelper

class TokenList(object):

    def __init__(self):
        self.d = dict()
    
    def insert(self, firebase_token: str, timestamp: str, device_token: str, nickname = ""):
        if not firebase_token in self.d:
            self.d[token] = (device_token, StringHelper.timestamp2Readable(timestamp), nickname)
        else:
            self.d[token] = (device_token, StringHelper.timestamp2Readable(timestamp), nickname)

    def delete(self, token: str):
        if token in self.d:
            self.d.pop(token)

    def getList(self):
        return [k for k, v in self.d.items()]

    def __str__(self):
        return str([k for k, v in self.d.items()])
    
    def isEmpty(self):
        return not bool(self.d)

    def getDict(self):
        return self.d

    def dump(self):
        # Make sure the file is only written by this method; otherwise you'll need a lock
        with open("registration.json", "w") as f:
            json.dump(self.d, f)

# Unit test
if __name__ == "__main__":
    t = TokenList()
    t.insert("1", time.time())
    t.insert("1", time.time())
    t.insert("2", time.time())
    t.insert("4", time.time())
    print(t)
    t.delete("1")
    print(t)