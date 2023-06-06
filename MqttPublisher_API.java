package mini_project;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class MqttPublisher_API implements MqttCallback{
	static MqttClient sampleClient;
	static String airportName="GMP";
	
    public static void main(String[] args) {
    	MqttPublisher_API obj = new MqttPublisher_API();
    	obj.run();
    }
    
    public void run() {
		connectBroker();
    	try {
    		sampleClient.subscribe("select");
    	} catch (MqttException e) {
    		e.printStackTrace();
    	}
    	    	
    	while(true) {    		
        	try {
            	String[] airportParking  = get_parking_percent();
            	 
            	publish_data("airport", "{\"airport\": "+airportParking[0]+"}");
            	publish_data("congestion", "{\"congestion\": "+airportParking[1]+"}");
            	publish_data("parking_name", "{\"parking_name\": "+airportParking[2]+"}");
            	publish_data("parking_percent", "{\"parking_percent\": "+airportParking[3]+"}");
            	publish_data("car_count", "{\"car_count\": "+airportParking[4]+"}");
            	publish_data("parking_count", "{\"parking_count\": "+airportParking[5]+"}");
           
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				try {
	    			sampleClient.disconnect();	    	        
	    		} catch (MqttException e) {    		
	    			e.printStackTrace();
	    		}
				e1.printStackTrace();
				System.out.println("Disconnected");
    	        System.exit(0);
			}        	    	
    	}
	}
    
    public void connectBroker() {
        String broker       = "tcp://127.0.0.1:1883"; // Broker Server IP
        String clientId     = "practice"; // Client ID
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            sampleClient = new MqttClient(broker, clientId, persistence); // Initialization of MQTT Client
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);//브로커서버에 접속
            sampleClient.setCallback(this); // Callback option 추가
            System.out.println("Connected");
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
    
    public static void publish_data(String topic_input, String data) {
        String topic        = topic_input;
        int qos             = 0;
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
    
    public static String[] get_parking_percent() {
    	String url = "http://openapi.airport.co.kr/service/rest/AirportParkingCongestion/airportParkingCongestionRT" // https가 아닌 http 프로토콜을 통해 접근해야 함
    			+ "?serviceKey=yVXWL9VZFvDwuNmuCT%2FYSw0fYOENEf49hUT52Qf3JLEI8jFk6eLgnDD34WoFeJGamB4LnaPrtytSShNsz9ZpFA%3D%3D"
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
		
    
		// 2.파싱
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		System.out.println(doc);
		
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

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		System.out.println("Connection Lost");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub		
		
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		// TODO Auto-generated method stub
		if (topic.equals("select")) {
			System.out.println("--------------Actuator Function--------------");
			System.out.println("Airport Name changed");
			System.out.println("SELECT: " + msg.toString());
			airportName=msg.toString();
			System.out.println("---------------------------------------------");
		}
	}    
}
