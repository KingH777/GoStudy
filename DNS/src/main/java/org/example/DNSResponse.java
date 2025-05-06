package org.example;

import java.util.ArrayList;
import java.util.List;

// DNS响应解析类：
class DNSResponse {
    private final byte[] responseData;// 存储响应数据的字节数组
    private int position;// 当前解析位置
    private short transactionId;// 事务ID
    private short flags;// 标志字段
    private short questionCount;// 问题记录数
    private short answerCount;// 回答记录数
    private short authorityCount;// 授权记录数
    private short additionalCount;// 附加记录数
    private final List<String> ipAddresses;// 存储解析出的IP地址列表
    //方法
    public DNSResponse(byte[] responseData) { //初始化并解析DNS响应
        this.responseData = responseData;
        this.position = 0;
        this.ipAddresses = new ArrayList<>();
        parseResponse();
    }

    // 解析DNS响应报文
    private void parseResponse() {
        try {
            // 解析DNS报文头部
            transactionId = readShort();
            flags = readShort();
            questionCount = readShort();
            answerCount = readShort();
            authorityCount = readShort();
            additionalCount = readShort();

            // 跳过问题部分
            for (int i = 0; i < questionCount; i++) {
                skipDomainName();
                position += 4; // 跳过查询类型和查询类别（各2字节）
            }

            // 解析回答部分
            for (int i = 0; i < answerCount; i++) {
                skipDomainName();// 跳过域名

                short type = readShort();
                short aClass = readShort();
                /* TTL */ readInt();
                short dataLength = readShort();

                if (type == 1 && aClass == 1) { // 如果是A记录（IPv4地址）且为IN类别

                    StringBuilder ip = new StringBuilder();
                    for (int j = 0; j < 4; j++) {
                        ip.append(responseData[position++] & 0xFF);
                        if (j < 3) ip.append(".");
                    }
                    ipAddresses.add(ip.toString());
                } else {
                    // 跳过其他类型的记录
                    position += dataLength;
                }
            }
        } catch (Exception e) {
            System.out.println("解析DNS响应报文时发生错误：" + e.getMessage());
        }
    }

    // 读取一个短整数（2字节，无符号）
    private short readShort() {
        short value = (short) ((responseData[position] & 0xFF) << 8 | (responseData[position + 1] & 0xFF));
        position += 2;
        return value;
    }
    // 读取一个整数（4字节，无符号）
    private void readInt() {
        int value = ((responseData[position] & 0xFF) << 24) |
                ((responseData[position + 1] & 0xFF) << 16) |
                ((responseData[position + 2] & 0xFF) << 8) |
                (responseData[position + 3] & 0xFF);
        position += 4;
    }
    // 跳过域名（处理DNS中的域名压缩）
    private void skipDomainName() {
        while (true) {
            int length = responseData[position] & 0xFF;
            // 处理压缩指针（前两位为11）
            if ((length & 0xC0) == 0xC0) {
                position += 2; // 跳过指针（2字节）
                return;
            }
            // 域名结束（长度为0）
            if (length == 0) {
                position++;
                return;
            }
            // 跳过常规标签
            position += length + 1;
        }
    }

    public short getTransactionId() {// 获取事务ID
        return transactionId;
    }

    public short getFlags() {// 获取标志字段
        return flags;
    }

    public short getQuestionCount() {// 获取问题记录数
        return questionCount;
    }

    public short getAnswerCount() {// 获取回答记录数
        return answerCount;
    }

    public short getAuthorityCount() { // 获取授权记录数
        return authorityCount;
    }

    public short getAdditionalCount() {// 获取附加记录数
        return additionalCount;
    }

    public List<String> getIPAddresses() { // 获取解析出的IP地址列表
        return ipAddresses;
    }



}


