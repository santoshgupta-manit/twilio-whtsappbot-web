
import static spark.Spark.post;
import static spark.Spark.get;

import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;

import spark.servlet.SparkApplication;

public class SmsApp implements SparkApplication{
	
	@Override
	public void init() {
		get("/hello", (req, res) -> "Hello World");
	}
	
	public static void main(String[] args) {
		//get("/", (req, res) -> "Hello Web");
		System.out.println(" application started : ");
		new SmsApp().init();
		post("/sms", (req, res) -> {
			int fromIndex = req.body().lastIndexOf("WaId=");
			int toIndex = req.body().indexOf("&", req.body().lastIndexOf("WaId="));
			String mobileNo = "whatsapp:+" + req.body().substring(fromIndex + 5, toIndex);
			fromIndex = req.body().lastIndexOf("Body");
			toIndex = req.body().indexOf("&", req.body().lastIndexOf("Body="));
			String reqBody = req.body().substring(fromIndex + 5, toIndex);
			System.out.println(" user input : "+reqBody);
			System.out.println(" last message : "+getLastMsg(mobileNo));
			String nextMsg = nextMsg(getLastMsg(mobileNo),reqBody,mobileNo);
			System.out.println(" next message : "+nextMsg);
			res.type("application/xml");
			// checkMessageHistory("");
			System.out.println("req : " + req.body());

			Body body = new Body.Builder(nextMsg).build();
			Message sms = new Message.Builder().body(body).build();
			MessagingResponse twiml = new MessagingResponse.Builder().message(sms).build();
			return twiml.toXml();
		});
	}

	private static String getLastMsg(String whtsAppNo) {
		Twilio.init("ACf52f89bcc2450064e9d273bb046b0c55", "610a6af5b1ae7161ef6c76efe4cb99f8");
		System.out.println("Checking History of conversation");
		ResourceSet<com.twilio.rest.api.v2010.account.Message> messages = com.twilio.rest.api.v2010.account.Message
				.reader()
				// .setDateSentAfter(
				// ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("UTC")))

				// .setTo(new com.twilio.type.PhoneNumber(whtsAppNo))
				// .setFrom(whtsAppNo)
				.setTo(whtsAppNo).limit(20).read();

		for (com.twilio.rest.api.v2010.account.Message record : messages) {
			System.out.println("message history : "+record);
			if (record.getBody().contains(
					"Greetings from Fedex.We are pleased to inform you that you have an upcoming delivery with tracking ID")
					|| record.getBody().contains("What date will you like us to delivery?")
					|| record.getBody().contains(
							"Thanks for confirming.Your date is updated. Do you want to change the delivery location as well?")
					|| (record.getBody().contains("The package with")
							&& record.getBody().contains("will be left with"))) {
				return record.getBody();
			}
		}
		return null;
	}
	
	private static String findTrackingId(String whtsAppNo)
	{
		Twilio.init("ACf52f89bcc2450064e9d273bb046b0c55", "610a6af5b1ae7161ef6c76efe4cb99f8");
		System.out.println("Checking History of conversation");
		ResourceSet<com.twilio.rest.api.v2010.account.Message> messages = com.twilio.rest.api.v2010.account.Message
				.reader()
				// .setDateSentAfter(
				// ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("UTC")))

				// .setTo(new com.twilio.type.PhoneNumber(whtsAppNo))
				// .setFrom(whtsAppNo)
				.setTo(whtsAppNo).limit(20).read();
		String trackingIDRecord = "";
		for (com.twilio.rest.api.v2010.account.Message record : messages) {
			
			if (record.getBody().contains(
					"Greetings from Fedex.We are pleased to inform you that you have an upcoming delivery with tracking ID")) {
				trackingIDRecord= record.getBody();
				break;
			}
		}
		System.out.println("trackingIDRecord : "+trackingIDRecord);
		int fromIndex = trackingIDRecord.lastIndexOf("tracking ID");
		int toIndex = trackingIDRecord.indexOf(".", trackingIDRecord.lastIndexOf("tracking ID"));
		System.out.println("fromIndex : "+fromIndex);
		System.out.println("toIndex : "+toIndex);
		return trackingIDRecord.substring(fromIndex,toIndex);
		
	}

	private static String nextMsg(String lastMsg, String inOption,String whtsAppNo) {
		System.out.println("in nextMsg method");
		System.out.println("lastMsg : "+lastMsg);
		System.out.println("inOption : "+inOption);
		String msg = "";
		if (lastMsg.contains(
				"Greetings from Fedex.We are pleased to inform you that you have an upcoming delivery with tracking ID")) {
			if (inOption.equalsIgnoreCase("Date")) {
				msg = "What date will you like us to delivery?" + "\n 28-Sep" + "\n 29-Sep" + "\n 30-Sep";
			} else {
				msg = "Please select a valid option";
			}

		} else if (lastMsg.contains("What date will you like us to delivery?")) {

			if (inOption.equalsIgnoreCase("28-Sep") || inOption.equalsIgnoreCase("29-Sep")
					|| inOption.equalsIgnoreCase("30-Sep")) {
				msg = "Thanks for confirming.Your date is updated. Do you want to change the delivery location as well?"
						+ "\nNeighbor \nRetail Shop \nDropbox";
			} else {
				msg = "Please select a valid option";
			}
		} else if (lastMsg.contains(
				"Thanks for confirming.Your date is updated. Do you want to change the delivery location as well?")) {
			if (inOption.equalsIgnoreCase("Neighbor") || inOption.equalsIgnoreCase("Retail Shop")
					|| inOption.equalsIgnoreCase("Dropbox")) {
				msg = "The package with <trackingID> will be left with " + inOption + ".";
				msg = msg.replace("<trackingID>", findTrackingId(whtsAppNo));
			} else {
				msg = "Please select a valid option";
			}
		} else if (lastMsg.contains("The package with") && lastMsg.contains("will be left with")) {
			msg = "Your options were already updated";
		}
		return msg;
	}

}
