package org.armanious.csci1260.obfuscation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public final class ObfuscatedNameGenerator {
	
	private final char[] validChars;
	private final int length;
	
	private final BigInteger base;
	private BigInteger maximum;
	private BigDecimal maximumAsDecimal;
	private final HashSet<BigInteger> used = new HashSet<>();
	private final Random random = new Random();
	
	public ObfuscatedNameGenerator(String pattern, int length){
		//currently, pattern only functions as the valid characters to use
		validChars = new char[pattern.length()];
		System.arraycopy(pattern.toCharArray(), 0, validChars, 0, pattern.length());
		Arrays.sort(validChars);
		for(int i = 0; i < validChars.length; i++){
			for(int j = i + 1; j < validChars.length; j++){
				if(validChars[i] == validChars[j]){
					throw new IllegalArgumentException("Pattern may have no duplicate characters.");
				}
			}
		}
		
		this.length = length;
		
		base = BigInteger.valueOf(validChars.length);
		maximum = BigInteger.ZERO;
		for(int i = 0; i < length; i++){
			maximum = maximum.multiply(base).add(base.subtract(BigInteger.ONE));
		}
		maximumAsDecimal = new BigDecimal(maximum);
	}
	
	public void addToUsedList(String name){
		BigInteger value = BigInteger.valueOf(0);
		for(int i = 0; i < name.length(); i++){
			int pos = Arrays.binarySearch(validChars, name.charAt(i));
			if(pos < 0){
				System.err.println("Warning: name " + name + " is not valid.");
				return;
			}
			value = value.add(BigInteger.valueOf(pos).multiply(base.pow(name.length() - i - 1)));
			
			//validChars: abcdefghij
			//defg = 3*10^3 + 4*10^2 +5*10^1 + 6*10^0
			//3456 = 3*10^3 + 4*10^2 + 5*10^1 + 6*10^0
			//3456 % 10 = 6
			//3456 / 10 = 345
		}
		used.add(value);
	}
	
	private String convertBigIntegerToString(BigInteger value){
		final char[] string = new char[length];
		int idx = length - 1;
		while(true){
			string[idx] = validChars[value.mod(base).intValue()];
			value = value.divide(base);
			if(value.signum() == 0 || idx == 0){
				break;
			}
			idx--;
		}
		while(idx != 0){ //pad beginning of String with the minimum value
			string[--idx] = validChars[0];
		}
		return new String(string);
	}
	
	public String getNext(){
		final long start = System.currentTimeMillis();
		long lastMarker = start;
		while(true){
			if(System.currentTimeMillis() - lastMarker > 5000){
				System.err.println("Warning: spent " + (System.currentTimeMillis() - start)/1000 + " seconds searching for new name");
				if(used.size() == maximum.intValue()){
					System.err.println("All names have been used!");
					throw new RuntimeException("All possible obfuscated names have been generated!\n"
							+ "Please extend length and try again or supply more valid characters.");
				}else{
					lastMarker = System.currentTimeMillis();
				}
			}
			BigInteger value = BigDecimal.valueOf(random.nextDouble()).multiply(maximumAsDecimal).toBigInteger();
			if(used.add(value)){
				return convertBigIntegerToString(value);
			}
		}
	}
	
	public void reset(){
		used.clear();
	}

}
