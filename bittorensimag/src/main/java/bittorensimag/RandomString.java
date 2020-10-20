package bittorensimag;

import java.util.Random;

public class RandomString {
	
	private String randomString;
	
	public RandomString() {
	    // create a string of all characters
	    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	    // create random string builder
	    StringBuilder sb = new StringBuilder();

	    // create an object of Random class
	    Random random = new Random();

	    // specify length of random string
	    int length = 10;

	    for(int i = 0; i < length; i++) {

	      // generate random index number
	      int index = random.nextInt(alphabet.length());

	      // get character specified by index
	      // from the string
	      char randomChar = alphabet.charAt(index);

	      // append the character to string builder
	      sb.append(randomChar);
	    }

	    this.randomString = sb.toString();
	}

	public String getRandomString() {
		return randomString;
	}

	@Override
	public String toString() {
		return "RandomString [randomString=" + randomString + "]";
	}

	
	
}
