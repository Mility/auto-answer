package com.github.fonoisrev.run;

import com.github.fonoisrev.bean.User;
import com.github.fonoisrev.data.QuestionsData;
import com.github.fonoisrev.data.UserData;
import org.java_websocket.drafts.Draft_6455;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.jsfr.json.compiler.JsonPathCompiler;
import org.jsfr.json.path.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

public class MyRunner implements CommandLineRunner {
    
    /** logger */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MyRunner.class);
    
    private static JsonSurfer SURFER = JsonSurferJackson.INSTANCE;
    
    private static JsonPath COUNT_PATH = JsonPathCompiler.compile("$..count");
    private static JsonPath SCORE_PATH = JsonPathCompiler.compile("$..score");
    
    @Autowired
    UserData userData;
    
    @Autowired
    QuestionsData questionsData;
    
    @Autowired
    RestTemplate template;
    
    @Override
    public void run(String... args) throws Exception {
        for (User user : userData.getUsers()) {
            String json = template.getForObject(
                    "http://api.yiqiapp.cn/dkdt/api/login/auto/" +
                    user.loginToken, String.class);
            LOGGER.info("Start User Receive {}", json);
            user.count = SURFER.collectOne(json, Integer.class, COUNT_PATH);
            for (int i = user.count; i > 0; --i) {
                LOGGER.info("{} 开始第 {} 次答题", user.name, i);
                MyWebSocketClient client =
                        new MyWebSocketClient(new URI("ws://eas.yiqiapp.cn/ws"),
                                              new Draft_6455(), user,
                                              questionsData);
                
                client.connect();
                client.join();
            }
            json = template.getForObject(
                    "http://api.yiqiapp.cn/dkdt/api/login/auto/" +
                    user.loginToken, String.class);
            LOGGER.info("End Receive {}", json);
            user.score = SURFER.collectOne(json, Integer.class, SCORE_PATH);
            LOGGER.info("{} 答题完成, 总分 {}", user.name, user.score);
        }
        
    }
}