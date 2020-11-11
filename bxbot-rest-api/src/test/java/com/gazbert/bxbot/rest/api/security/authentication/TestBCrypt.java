package com.gazbert.bxbot.rest.api.security.authentication;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {

  private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

  /**
   * Generate an encrypted password.
   */
  public static void main(String[] args) {

    encrypt("some string to encrypt");
  }

  private static void encrypt(String s) {
    System.out.println(s + " -> '" + ENCODER.encode(s) +  "'");
  }

}
