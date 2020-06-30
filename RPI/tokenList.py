import time

class TokenList(object):

    def __init__(self):
        self.d = dict()
    
    def insert(self, token: str, timestamp: str, nickname = ""):
        if not token in self.d:
            self.d[token] = (timestamp, nickname)
        else:
            pass

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