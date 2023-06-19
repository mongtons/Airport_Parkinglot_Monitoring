# Airport_Parkinglot_Monitoring
한림대학교 2023학년도 1학기 IoT네트워크 미니프로젝트
## 개요
&nbsp;한림대학교 2023학년도 1학기 IoT네트워크 과목의 프로젝트로, MQTT를 활용한 웹페이지 통신을 목적으로 한다. 기존 사물인터넷 서비스 중 주차장과 관련된 서비스는 적다고 느껴졌다. 이러한 상황은 관광지나, 쇼핑몰 등 대형주차장조차 협소하다고 느껴진다. 차량을 가지고 해당 주차장 정보를 얻지 못하여 방문했다가 만차로 뒷걸음질하게 된 경우가 있을 것이다. 이러한 상황을 방지하기 위해 전국의 모든 주차장 정보는 아니지만, 주중에도 이용률이 가장 많은 공항 주차장을 이용하여 프로젝트를 진행한다. 공항 주차장 상태 정보를 얻어 발걸음을 뒤로한 사용자가 감소할 수 있게 유도하는 목적이 있다.
## 개발 환경
* Node.js `18.16.0 LTS`
* Mosquitto `2.0.15`
* Jquery `3.3.1`
* Java `JDK 1.8`
* VS Code `1.78.2`
* Mongo DB `1.37.0`
* eclipse `2022-12 (4.24.0)`
* templete `https://templatemo.com/tm-524-product-admin`
* Topic `https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15063437`
## 프로젝트 구조
&nbsp;이 프로젝트는 ‘공공데이터포털’의 ‘한국공항공사_전국공항 주차장 혼잡도’ 데이터를 활용한 프로젝트이다. 인증키를 활용하여 XML형식의 데이터를 MQTT 통신을 통해 REST를 한다. XML 데이터에서 원하는 데이터를 Node.js로 구동하는 HTML 웹 페이지에 출력하는 방식이다. 또한 원하는 공항명을 웹 페이지에서 dropdown 버튼으로 선택하면 선택된 공항 문자열은 MQTT broker에 publish 한다. 해당 문자열은 subscribe하는 사용자가 받게 되고, 받은 공항명 문자열을 이용해 ‘한국공항공사_전국공항 주차장 혼잡도’에서 받은 공항명의 주차장 데이터를 웹사이트에 출력하는 방식이다.
## 실행 요약
&nbsp;프로젝트는 MQTT 통신을 이용하여 실행한다. Broker는 Mosquitto를 사용한다. 웹 페이지에 데이터 출력하기 위해 데이터베이스를 사용하는데 MongoDB를 사용했다. 우선 mosquitto와 Node.js가 실행되고 있는 상태여야한다.   
&nbsp;우선 Topic을 얻기 위해 [공공데이터 포털](https://www.data.go.kr/)에서 [한국공항공사_전국공항 주차장 혼잡도](https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15063437) Xml데이터를 Topic으로 사용한다. 아래 코드를 이용해 '공항명', '주차장이름'과 같은 Topic을 Xml데이터에서 가져온다.
```java
public static String[] get_parking_percent() {
  String url = "http://openapi.airport.co.kr/service/rest/AirportParkingCongestion/airportParkingCongestionRT"
    + "?serviceKey="
    + "&numOfRows=1000"
    + "&pageNo=1"
    + "&schAirportCode="+airportName;
    	    	
	String airport = "";
	String parking_name="";
	String parking_percent = "";
	String congestion="";
	String car_count="";
	String parking_count="";
				
  Document doc = null;

	try {
		doc = Jsoup.connect(url).get();
	} catch (IOException e) {
		e.printStackTrace();
	}
		
	Elements elements = doc.select("item");
	for (Element e : elements) {
		airport = "\""+e.select("airportKor").text()+"\"";
		congestion="\""+e.select("parkingCongestion").text()+"\"";
		parking_name="\""+e.select("parkingAirportCodeName").text()+"\"";
		parking_percent = "\""+e.select("parkingCongestionDegree").text()+"\"";
		car_count=e.select("parkingOccupiedSpace").text();
		parking_count = e.select("parkingTotalSpace").text();
		break;
	}
	String[] out = {airport, congestion, parking_name, parking_percent, car_count, parking_count};
  return out;
}
```
&nbsp;가져온 데이터를 사용자(Client)는 해당 Topic를 Broker에게 Publish한다.
```java
String[] airportParking  = get_parking_percent();
publish_data("airport", "{\"airport\": "+airportParking[0]+"}");
```
```java
public static void publish_data(String topic_input, String data) {
  String topic = topic_input;
  int qos = 0;
  try {
    System.out.println("Publishing message: "+data);
    sampleClient.publish(topic, data.getBytes(), qos, false);
    System.out.println("Message published");
  } catch(MqttException me) {
    System.out.println("reason "+me.getReasonCode());
    System.out.println("msg "+me.getMessage());
    System.out.println("loc "+me.getLocalizedMessage());
    System.out.println("cause "+me.getCause());
    System.out.println("excep "+me);
    me.printStackTrace();
  }
}
```
&nbsp;Topic을 받은 Broker는 해당 Topic을 구독하는 Subscriber(Client)에게 Publish를 한다. 해당 동작을 하는 것이 Mosquitto 이다. Subscriber는 VS Code 상에 생성하였다. Mqtt와 연결하고 연결이 성공한다면, 해당 Topic의 이름을 사용하여 `client.subscribe("airport")` Topic을 구독한다.
```javascript
/**
* MQTT subscriber (MQTT Server connection & Read resource data)
*/
var mqtt = require("mqtt");
const { stringify } = require('querystring');
var client = mqtt.connect("mqtt://127.0.0.1")
 
client.on("connect", function(){
  client.subscribe("airport");
  console.log("Subscribing airport");
  client.subscribe("congestion");
  console.log("Subscribing congestion");
  client.subscribe("parking_name");
  console.log("Subscribing parking_name");
  client.subscribe("parking_percent");
  console.log("Subscribing parking_percent");
  client.subscribe("car_count");
  console.log("Subscribing car_count");
  client.subscribe("parking_count");
  console.log("Subscribing parking_count");
})
```
&nbsp;구독한 Topic을 웹 페이지에 사용하기 위해 MongoDB에 저장한다. 또한 저장된 데이터를 사용하기 위해 socket을 이용하여 HTML코드 상으로 전송한다. 전송된 데이터를 코드상에서 출력하게 된다면 아래와 같은 결과가 나온다.
![image01]()
