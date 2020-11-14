package com.github.wxpay.sdk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;

/**
 * @author constantinejohn
 */
@Component
public class MyWXPayConfig extends WXPayConfig {

    @Value("application.app-id")
    private String appId;

    @Value("application.mch-id")
    private String mchId;

    @Value("application.key")
    private String key;

    @Value("application.cert-path")
    private String certPath;

    private byte[] certData;

    /**
     * 读取商户的数字证书，并付值到数组里
     *
     * @see PostConstruct @PostConstruct该注解被用来修饰一个非静态的void（）方法。
     * 被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行，并且只会被服务器执行一次。
     * PostConstruct在构造函数之后执行，init（）方法之前执行。
     */
    @PostConstruct
    public void init() throws Exception {
        //读取文件
        File file = new File(certPath);
        FileInputStream inputStream = new FileInputStream(file);
        BufferedInputStream stream = new BufferedInputStream(inputStream);
        // 初始化 数组
        this.certData = new byte[(int) file.length()];
        // 读取流的内容付值到数组里
        stream.read(this.certData);
        stream.close();
        inputStream.close();
    }

    @Override
    String getAppID() {
        return appId;
    }

    @Override
    String getMchID() {
        return mchId;
    }

    @Override
    String getKey() {
        return key;
    }

    @Override
    InputStream getCertStream() {
        return new ByteArrayInputStream(this.certData);
    }

    @Override
    public int getHttpConnectTimeoutMs() {
        return super.getHttpConnectTimeoutMs();
    }

    @Override
    public int getHttpReadTimeoutMs() {
        return super.getHttpReadTimeoutMs();
    }

    /**
     * 这个写法固定的
     *
     * @return
     */
    @Override
    IWXPayDomain getWXPayDomain() {
        return new IWXPayDomain() {
            @Override
            public void report(String domain, long elapsedTimeMillis, Exception ex) {

            }

            @Override
            public DomainInfo getDomain(WXPayConfig config) {
                return new IWXPayDomain.DomainInfo(WXPayConstants.DOMAIN_API, true);
            }
        };
    }

    @Override
    public boolean shouldAutoReport() {
        return super.shouldAutoReport();
    }

    @Override
    public int getReportWorkerNum() {
        return super.getReportWorkerNum();
    }

    @Override
    public int getReportQueueMaxSize() {
        return super.getReportQueueMaxSize();
    }

    @Override
    public int getReportBatchSize() {
        return super.getReportBatchSize();
    }
}
