# coding:utf-8
# 请提前安装第三方库websocket-client
# access_id 与 access_ke 已留空，填入即可
# 运行时请放入wav所在文件夹内, 脚本会自行生成目录文件WavList.txt
# 结果输出在Result.txt


import thread
import time
import hashlib
import websocket
import json
import uuid
import os
import sys

access_id = ""
access_key = ""
url = "wss://asr.yuzhix.com/api/DecodeAudio?"


class Clinent():
    def __init__(self):
        pass

    def create_connection(self, URL):
        # 建立wss连接
        self.ws = websocket.create_connection(
            URL, sslopt={"check_hostname": False}, header=["online: True"])

    def send(self, content):
        # 间隔1s发送请求
        time.sleep(1)
        self.ws.send_binary(content)

    def recv(self):
        # 接收
        result = self.ws.recv()
        return str(result)


def md5hash(stringT):
    # md5加密
    sign = hashlib.md5()
    sign.update(stringT)
    return sign.hexdigest()


def makeURL(access_id, access_key):
    # 生成完整URL
    nonce_str = uuid.uuid4()
    request_id = uuid.uuid1()
    stringA = "access_id=%s&nonce_str=%d&request_id=%d&key=%s" % (
        access_id, nonce_str, request_id, access_key)
    turl = url + stringA + "&sign=" + md5hash(stringA)
    return turl


def makeWavList():
    # 打印当前目录内*.pcm文件到WavList.txt
    reload(sys)
    sys.setdefaultencoding('utf-8')
    output = open("WavList.txt", "w")
    pwd = os.listdir(os.getcwd())
    for path in pwd:
        if os.path.isfile(path) and path[-4:] == ".pcm":
            output.write("%s/%s\n" % (sys.path[0], path))
    output.close()


def decode(rec):
    # 调整输出格式
    result = rec
    return json.dumps(json.loads(result), ensure_ascii=False, indent=4)


def main():
    makeWavList()
    input = open("WavList.txt", "r")
    output = open("Result.txt", "w")
    testNum = 0
    test = Clinent()
    try:
        for line in input:
            testNum = testNum + 1
            path = line.replace('\n', '')
            print("Test%d :%s" % (testNum, path))
            output.write("Test%d :%s\n" % (testNum, path))
            turl = makeURL(access_id, access_key)
            test.create_connection(turl)
            file = open(path, "rb")
            content = file.read()
            test.send(content)
            result = test.recv()
            output.write("%s\n" % (decode(result)))
    finally:
        input.close()
    output.close()


if __name__ == '__main__':
    main()
