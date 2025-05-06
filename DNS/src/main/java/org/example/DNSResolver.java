package org.example;

// Main Application Class
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

// 声明GUI组件
public class DNSResolver extends JFrame {
    private final JTextField domainField;
    private final JTextArea resultArea;
    private final JLabel statusLabel;

    // 定义DNS服务器IP和端口（使用Google的公共DNS服务器）
    private static final String DNS_SERVER = "8.8.8.8";
    private static final int DNS_PORT = 53;


    public DNSResolver() {

        // 设置窗口基本属性
        setTitle("DNS 解析器");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel domainLabel = new JLabel("域名:");
        domainField = new JTextField(30);
        inputPanel.add(domainLabel, BorderLayout.WEST);
        inputPanel.add(domainField, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton resolveButton = new JButton("解析");
        JButton clearButton = new JButton("清除");
        buttonPanel.add(resolveButton);
        buttonPanel.add(clearButton);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // 创建结果面板
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JLabel resultLabel = new JLabel("查询结果:");
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(580, 280));

        resultPanel.add(resultLabel, BorderLayout.NORTH);
        resultPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建状态面板
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        statusLabel = new JLabel("就绪");
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // 将所有面板添加到主窗口
        add(inputPanel, BorderLayout.NORTH);
        add(resultPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        // 添加事件监听器
        resolveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resolveDomain();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                domainField.setText("");
                resultArea.setText("");
                statusLabel.setText("就绪");
            }
        });

        // 为域名输入框添加键盘事件监听器
        domainField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    resolveDomain();
                }
            }
        });
    }

    //处理用户输入的域名并启动DNS查询
    private void resolveDomain() {
        String domain = domainField.getText().trim();// 获取输入框中的域名并去除首尾空格

        if (domain.isEmpty()) {   //查询是否为空
            JOptionPane.showMessageDialog(this, "请输入域名", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusLabel.setText("正在解析 " + domain + "...");
        resultArea.setText("");
        /*
          在后台线程中执行DNS查询！
         */
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    return performDNSQuery(domain);  // 执行DNS查询并返回结果
                } catch (Exception e) {
                    return "错误: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    resultArea.setText(result);
                    statusLabel.setText("完成");
                } catch (Exception e) {
                    resultArea.setText("错误: " + e.getMessage());
                    statusLabel.setText("发生错误");
                }
            }
        };

        worker.execute();
    }

    //构建DNS查询报文，发送到DNS服务器，解析响应
    private String performDNSQuery(String domain) {
        StringBuilder result = new StringBuilder();
        try {
            result.append("查询域名: ").append(domain).append("\n\n");// 添加查询域名信息

            // 创建DNS查询报文
            DNSPacket queryPacket = new DNSPacket(domain);
            byte[] queryData = queryPacket.getBytes();

            // 创建UDP套接字并发送查询
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(5000);

            InetAddress dnsServer = InetAddress.getByName(DNS_SERVER);// 根据IP地址获取DNS服务器的InetAddress对象
            DatagramPacket packet = new DatagramPacket(queryData, queryData.length, dnsServer, DNS_PORT);// 创建UDP数据包

            result.append("正在向DNS服务器发送查询: ").append(DNS_SERVER).append("\n");
            socket.send(packet);

            // 接收响应
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

            result.append("等待响应...\n");
            socket.receive(responsePacket);// 接收响应数据包

            // 解析响应
            byte[] responseData = new byte[responsePacket.getLength()];// 创建一个大小刚好的数组用于存储响应数据
            System.arraycopy(responsePacket.getData(), 0, responseData, 0, responsePacket.getLength());

            result.append("收到响应. 正在解析...\n\n");
            DNSResponse response = new DNSResponse(responseData);// 创建DNS响应对象并解析响应数据

            // 处理并显示结果
            result.append("响应信息:\n");
            result.append("交易ID: 0x").append(String.format("%04X", response.getTransactionId())).append("\n");
            result.append("标志: 0x").append(String.format("%04X", response.getFlags())).append("\n");
            result.append("问题数: ").append(response.getQuestionCount()).append("\n");
            result.append("回答数: ").append(response.getAnswerCount()).append("\n");
            result.append("授权记录数: ").append(response.getAuthorityCount()).append("\n");
            result.append("附加记录数: ").append(response.getAdditionalCount()).append("\n\n");

            result.append("查询部分:\n");
            result.append(domain).append(" IN A\n\n");

            result.append("回答部分:\n");
            List<String> ipAddresses = response.getIPAddresses();

            if (ipAddresses.isEmpty()) {
                result.append("未找到此域名的IP地址.\n");
            } else {
                for (String ip : ipAddresses) {
                    result.append(domain).append(" IN A ").append(ip).append("\n");
                }
            }

            socket.close();

        } catch (SocketTimeoutException e) {
            result.append("错误: DNS查询超时\n");// 处理套接字超时异常
        } catch (UnknownHostException e) {
            result.append("错误: 无法连接到DNS服务器\n");
        } catch (Exception e) {
            result.append("错误: ").append(e.toString()).append("\n");
        }

        return result.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DNSResolver app = new DNSResolver();
                app.setVisible(true);
            }
        });
    }
}


