package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.VirtualDomain;
import com.example.demo.entity.VirtualUser;
import com.example.demo.form.UserForm;
import com.example.demo.repository.VirtualDomainRepository;
import com.example.demo.repository.VirtualUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional // 基本は書き込み可
public class UserService {

    private final VirtualDomainRepository domainRepository;
    private final VirtualUserRepository userRepository;

    // --- ★ここから追加: 読み取り・削除メソッド ---

    // ユーザ一覧取得 (読み取り専用)
    @Transactional(readOnly = true)
    public List<VirtualUser> findAllUsers() {
        return userRepository.findAll();
    }

    // ドメイン一覧取得 (読み取り専用)
    @Transactional(readOnly = true)
    public List<VirtualDomain> findAllDomains() {
        return domainRepository.findAll();
    }

    // ID検索
    @Transactional(readOnly = true)
    public VirtualUser findUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("指定されたユーザが見つかりません ID:" + id));
    }
    
    // 削除
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }
    
 // ★追加: 全ユーザを一括削除する
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }
    
 // ★追加: 検索機能
    @Transactional(readOnly = true)
    public List<VirtualUser> searchUsers(String keyword) {
        return userRepository.findByEmailContaining(keyword);
    }

    // --- ここまで追加 ---


    // --- 以下、既存のロジック (そのまま) ---

    public void saveUserFromForm(UserForm form, VirtualUser user) {
        VirtualDomain domain = domainRepository.findById(form.getDomainId()).orElseThrow();
        registerUser(user, domain, form.getUsername(), form.getPassword());
    }

    public void registerUser(VirtualUser user, VirtualDomain domain, String username, String password) {
       
    	// 1. 【追加】文字数チェック (2文字以上、30文字以内)
    	if (username.length() < 2 || username.length() > 30) {
            throw new IllegalArgumentException("ユーザ名は2文字以上、30文字以内で入力してください");
        }
    	
    	 // 文字種チェック
        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("ユーザ名に使用できない文字が含まれています (半角英数字 . _ - のみ使用可)");
        }
        
     // 3. 【追加】先頭と末尾の記号チェック (英数字で始まり、英数字で終わるべき)
        // matchesの ^[a-zA-Z0-9] は「先頭は英数字」
        // .* は「途中はなんでも」
        // [a-zA-Z0-9]$ は「末尾は英数字」
        if (!username.matches("^[a-zA-Z0-9].*[a-zA-Z0-9]$")) {
            throw new IllegalArgumentException("ユーザ名の先頭と末尾には、記号 (. _ -) は使用できません");
        }
        
     // 4. 【追加】ドットの連続チェック (..)
        if (username.contains("..")) {
            throw new IllegalArgumentException("ドット(.)を連続して使用することはできません");
        }

        String email = username + "@" + domain.getName();

        // 重複チェック
        VirtualUser existingUser = userRepository.findByEmail(email);
        if (existingUser != null) {
            if (user.getId() == null || !user.getId().equals(existingUser.getId())) {
                throw new IllegalArgumentException("そのメールアドレスは既に登録されています: " + email);
            }
        }

        user.setVirtualDomain(domain);
        if (password != null && !password.isEmpty()) {
            user.setPassword(password);
        }
        user.setEmail(email);
        String maildir = domain.getName() + "/" + username + "/";
        user.setMaildir(maildir);

        userRepository.save(user);
    }
}