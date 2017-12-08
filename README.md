
## FFASR 接入指南

 - API: wss://asr.yuzhix.com/api/DecodeAudio?[parameters]
 
### 业务流程
  1. 客户端向服务器发送带有相关验证参数的websocket请求
  1. 服务器通过验证后，保持当前连接并自行判断音频结束进行识别
  1. 服务器通过原连接返回识别结果

### 报文格式
  为确保数据安全，服务器只接受wss请求(Websocket + SSL)。请求内容包含：
  
  * 请求参数 （用于进行身份验证，参见 __身份验证机制__ ）
  - 报头 （用于告知asr音频相关信息， 参见 __报头信息__ ）
  - 二进制的报文Body（Audio buffer， __单通道音频数据__ ） 

### 报头信息
  报头信息需要包括以下字段：
  
  * __uuid__ : 当前连接的全局唯一标识符
  * __online  true/false__ 该音频是否是在线音频，在线音频即为正在录制的音频; 离线音频为单个的pcm/wav文件。目前仅支持在线音频。
  
### 身份认证机制
  客户端将下列字段以请求参数的形式传递给服务器以便完成身份信息校验：

  1. access_id  客户id
  1. nonce_str  随机字符串，不超过32位的随机字符串  
  1. uuid 该连接的唯一Global标识符，该uuid应与报文头的uuid一致 
  1. sign md5签名
 
##### 签名生成规则：

  1.  客户将拥有自己的access_key，按下列方式生成签名：
  2. 将参数名ASCII码从小到大排序（字典序，区分大小写），使用url键值对格式,即:
```
    key0=value0&key1=value1&key2=value2&…
```
  3. 将access_key拼接至末尾： (假设access_key为123456)
```  
        StringA=”access_id=xxxxxx&nonce_str=yyyyyy&uuid=000000”  //参数名按ascii排序
        SignTempString = StringA + “&key=123456“
```                                 
  4.	生成md5签名，即:
```
        sign=md5hash(SignTempString)
```
  5. 完整url示例：
```
    wss://asr.yuzhix.com/api/DecodeAudio?access_id=xxxxx&nonce_str=yyyyy&uuid=0000&sign=md5hash_value
```


 
