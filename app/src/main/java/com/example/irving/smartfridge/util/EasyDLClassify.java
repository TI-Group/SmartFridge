package com.example.irving.smartfridge.util;




import java.io.IOException;
import java.util.HashMap;
import org.json.JSONObject;
import com.baidu.aip.http.AipRequest;
import com.baidu.aip.http.EBodyFormat;
import com.baidu.aip.http.Headers;
import com.baidu.aip.http.HttpCharacterEncoding;
import com.baidu.aip.http.HttpContentType;
import com.baidu.aip.imageclassify.AipImageClassify;
import com.baidu.aip.util.Base64Util;
import com.baidu.aip.util.Util;

public class EasyDLClassify extends AipImageClassify{
    //接口地址
    private static final String EasyDL_URL="https://aip.baidubce.com/rpc/2.0/ai_custom/v1/classification/smartFridge";
    private static final String appId = "11435333";
    private static final String apiKey = "Wu9kXlRrRM9QdTdO4IlmiMrf";
    private static final String secretKey = "6Kbn0G7eATrBv0MKvvFlBy2S8vHu7MGn";

    public EasyDLClassify() {
        super(appId, apiKey, secretKey);
    }

    /**
     * EasyDL接口调用
     * @param image  图片的base64内容
     * @param top_num  返回的个数
     * @return JSONObject
     */
    public JSONObject easydlClassify(String image, int top_num) {
        AipRequest request = new AipRequest();
        preOperation(request);
        request.addBody("image", image);
        request.addBody("top_num", top_num);
        request.setUri(EasyDL_URL);
        request.addHeader(Headers.CONTENT_ENCODING,
                HttpCharacterEncoding.ENCODE_UTF8);
        request.addHeader(Headers.CONTENT_TYPE, HttpContentType.JSON_DATA);
        request.setBodyFormat(EBodyFormat.RAW_JSON);
        postOperation(request);
        return requestServer(request);
    }
    /**
     * EasyDL接口调用
     * @param file  图片的二进制数据
     * @param top_num  返回的个数
     * @return JSONObject
     */
    public JSONObject easydlClassify(byte[] file, int top_num) {
        String image = Base64Util.encode(file);
        return easydlClassify(image, top_num);
    }
    /**
     * EasyDL接口调用
     * @param filePath  图片的本地路径
     * @param top_num  返回的个数
     * @return JSONObject
     * @throws Exception
     */
    public JSONObject easydlClassifyByFilePath(String filePath, int top_num) throws Exception {
        String image = Base64Util.encode(Util.readFileByBytes(filePath));
        return easydlClassify(image, top_num);
    }
}