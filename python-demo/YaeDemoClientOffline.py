# -*- coding: utf-8 -*-
# 请提前安装第三方库websocket-client(pip3 install websocket-client)
# access_id 与 access_ke 已留空，填入即可
# 运行时请把pcm文件放入test-case文件夹内

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


class Client():
    def __init__(self):
        pass

    def create_connection(self, URL):
        # 建立wss连接
        self.ws = websocket.create_connection(
            URL, sslopt={"check_hostname": False}, header=["online: False"])

    def send(self, content):
        # 间隔1s发送请求
        
        offset=0
        while offset<len(content):
            size=6400
            if (len(content)-offset<size):
                size = len(content)-offset
            self.ws.send_binary(content[offset:offset+size])
            offset+=size
        self.ws.send("finish!")
        time.sleep(1)

    def recv(self):
        # 接收
        result = self.ws.recv()
        return str(result)


def md5hash(stringT):
    # md5加密
    sign = hashlib.md5(stringT.encode("utf8"))
    return sign.hexdigest()


def makeURL(access_id, access_key):
    # 生成完整URL
    nonce_str = uuid.uuid4()
    request_id = uuid.uuid1()
    stringA = "access_id=%s&nonce_str=%d&request_id=%d&key=%s" % (
        access_id, nonce_str, request_id, access_key)
    turl = url + stringA + "&sign=" + md5hash(stringA)
    return turl


def decode(rec):
    # 调整输出格式
    result = rec
    return json.dumps(json.loads(result), ensure_ascii=False, indent=4)


def testCase():
    # 找到test-case文件夹
    return os.path.dirname(os.getcwd()) + "/test-case"


def main():
    testNum = 0
    test = Client()
    wavPath = testCase()
    for pcm in os.listdir(wavPath):
        if pcm[-4:] == ".pcm":
            testNum = testNum + 1
            path = wavPath + "/" + pcm
            turl = makeURL(access_id, access_key)
            test.create_connection(turl)
            file = open(path, "rb")
            content = file.read()
            offset = 0
            '''
                '''
            test.send(content)
            result = test.recv()
            print("Test%d :%s\n%s\n" % (testNum, path, decode(result)))


if __name__ == '__main__':
    main()
