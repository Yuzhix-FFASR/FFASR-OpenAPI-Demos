
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
  - 二进制的报文Body（Audio buffer， __单通道音频, 16bit, 采样率16KHz__ ） 

### 报头信息
  报头信息仅需包含以下字段:
  
  * __online  true/false__ 该音频是否是在线音频，在线音频即为正在录制的音频; 离线音频为单个的pcm/wav文件。目前仅支持在线音频。
  
### 身份认证机制
  调用FFASR前需经过严格的鉴权验证。在处理用户请求前，服务端会通过请求参数进行签名校验以确保用户请求在传输过程中没有被恶意篡改或替换。客户端将下列字段以请求参数的形式传递给服务器以便完成身份信息校验：

 | 名称 | 类型 | 说明 |
 |------|------|------|
 | access_id | string | 客户id |
 | nonce_str | string | 32位随机字符串 |
 | request_id | string | 识别请求id，该uuid应与报文头的uuid一致  |
 | sign | string | 连接参数的md5签名，相见 __签名生成规则__ |
 
##### 签名生成规则：

  1.  客户将拥有自己的access_key，按下列方式生成签名：
  2. 将参数名ASCII码从小到大排序（字典序，区分大小写），使用url键值对格式,即:
```
    key0=value0&key1=value1&key2=value2&…
```
  3. 将access_key拼接至末尾： (假设access_key为123456)
```
        StringA="access_id=xxxxxx&nonce_str=yyyyyy&request_id=000000"  //参数名按ascii排序
        SignTempString = StringA + "&key=123456"
```
  4.    生成md5签名，即:
```
        sign=md5hash(SignTempString)
```
  5. 完整url示例：
  ```
wss://asr.yuzhix.com/api/DecodeAudio?access_id=xxxxx&nonce_str=yyyyy&uuid=0000&sign=md5hash_value
```
### 识别结果返回
请求结果以application/json格式在Response Body中返回；其他的HTTP错误码表示识别失败，具体的错误消息以application/json格式在Response Body中返回。

####  __识别成功__


 | 名称 | 类型 | 说明 |
 |------|------|------|
 | request_id | string | 识别请求id |
 | result | string | 识别结果 |
 * __ Sample __
```
{
    "request_id":"request_sample_0",
    "result":"测试音频啊哈哈"
}
```

####  __识别失败__


 | 名称 | 类型 | 说明 |
 |------|------|------|
 | request_id | string | 识别请求id |
 | error_code | string | 错误代码 |
 | error_msg | string | 具体错误信息 |
 
 * __Sample__
 ```
 {
    "request_id":"request_sample_0",
    "error_code"：1,
    "error_message":"无法识别有效音频"
}
```

 *  __错误代码定义__


 | 错误码 | 语义 |
 |------|------|
 | 1 | 无法识别有效音频 |
 | 2 | 身份验证失败 |
 | 3 | 调用过于频繁 |
 | 4 | 服务器忙 |
 | 5 | 内部错误 |
 
 ### _注意事项_
 1. FFASR现处于邀请测试阶段，希望申请测试的用户请发邮件至 bigvan@yuzhix.com
 1. 用户不宜过于频繁调用接口，现阶段支持单个access_id的调用频率不得高于1次/秒
