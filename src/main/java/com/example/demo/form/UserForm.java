package com.example.demo.form;

import lombok.Data;

@Data
public class UserForm {

	
	private Integer domainId;
    private String username;
    private String password;
    private String confirmPassword;

}
