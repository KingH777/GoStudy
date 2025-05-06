package org.example;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

// 创建DNS查询报文
class DNSPacket {
    private final short transactionId;// 事务ID，用于匹配请求和响应
    private final short flags;// 标志字段，指示查询类型等
    private final short questions;// 问题记录数
    private final short answerRRs;// 回答资源记录数
    private final short authorityRRs; // 授权资源记录数
    private final short additionalRRs;// 附加资源记录数
    private final String queryDomain; // 查询的域名
    private final short queryType;// 查询类型
    private final short queryClass;// 查询类别

    //方法
    public DNSPacket(String domain) {
        // 生成一个随机的事务ID
        this.transactionId = (short) (new Random().nextInt(65535));
        this.flags = 0x0100;
        this.questions = 1;
        this.answerRRs = 0;
        this.authorityRRs = 0;
        this.additionalRRs = 0;
        this.queryDomain = domain;
        this.queryType = 1;   // A记录（IPv4地址）
        this.queryClass = 1;  // IN类别（Internet）
    }

    // 将DNS查询报文转换为字节数组，用于网络传输
    public byte[] getBytes() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // 写入DNS报文头部
            dos.writeShort(transactionId);
            dos.writeShort(flags);
            dos.writeShort(questions);
            dos.writeShort(answerRRs);
            dos.writeShort(authorityRRs);
            dos.writeShort(additionalRRs);

            // 写入查询域名（使用DNS格式：长度前缀+标签）
            String[] parts = queryDomain.split("\\.");// 按点分割域名
            for (String part : parts) {
                dos.writeByte(part.length());
                dos.writeBytes(part);
            }
            dos.writeByte(0); // 写入一个0字节表示域名结束

            // 写入查询类型和查询类别
            dos.writeShort(queryType);
            dos.writeShort(queryClass);

            dos.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }


}
