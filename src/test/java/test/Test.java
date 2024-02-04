package test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.time.StopWatch;
import velohttp.VelohttpClient;
import velohttp.config.RequestConfig;
import velohttp.entity.HttpUriRequest;
import velohttp.entity.HttpUriResponse;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {

	static VelohttpClient velohttpClient = new VelohttpClient();

	public static void main(String[] args) {
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		StopWatch started = StopWatch.createStarted();

		for (int j = 0; j < 1; j++) {
			int number = 1000;
			CountDownLatch countDownLatch = new CountDownLatch(number);

			for (int i = 0; i < number; i++) {
				int finalI = i;
				executorService.execute(() -> {
					doExecute(finalI);
					countDownLatch.countDown();
				});
			}

			try {
				countDownLatch.await();
				System.out.println(started.getTime());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

	}

	public static void doExecute(int index) {
		HttpUriRequest.HttpGetRequest httpGetRequest = new HttpUriRequest.HttpGetRequest("http://192.168.10.110:9809/member/member/info/v1/testGetId?id=" + index);
		RequestConfig requestConfig = new RequestConfig();
		requestConfig.setConnectTimeout(10000);
		requestConfig.setSocketTimeout(10000);
		httpGetRequest.setRequestConfig(requestConfig);
		HttpUriResponse execute = velohttpClient.execute(httpGetRequest);
		JSONObject jsonObject = JSON.parseObject(execute.getEntity());
		if (!Objects.equals(jsonObject.getInteger("data"), index)) {
			System.out.println(index + "/" +execute.getEntity());
		}
	}


}
