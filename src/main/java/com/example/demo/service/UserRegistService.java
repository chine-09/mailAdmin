package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.entity.VirtualDomain;
import com.example.demo.entity.VirtualUser;
import com.example.demo.form.UserForm;
import com.example.demo.repository.VirtualDomainRepository;
import com.example.demo.repository.VirtualUserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRegistService {
	private final VirtualDomainRepository domainRepository;
	private final VirtualUserRepository userRepository;
	
	public void saveUserFromForm(UserForm form, VirtualUser user) {
        VirtualDomain domain = domainRepository.findById(form.getDomainId()).orElseThrow();
        // 共通ロジックを呼び出す
        registerUser(user, domain, form.getUsername(), form.getPassword());
    }
	
	public void registerUser(VirtualUser user, VirtualDomain domain, String username, String password) {
		
		// 半角英数字、ドット、アンダースコア、ハイフン以外が含まれていたらエラー
        if (!username.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("ユーザ名に使用できない文字が含まれています (半角英数字 . _ - のみ使用可)");
        }
        
		String email = username + "@" + domain.getName();

        // ★追加: 重複チェックロジック
        VirtualUser existingUser = userRepository.findByEmail(email);
        
        // 「既にそのメアドが存在し」かつ「それが自分自身ではない（新規登録 or 他人のメアド）」場合
        if (existingUser != null) {
            // 新規登録(idがnull) または 更新時に別人のIDと被っている場合
            if (user.getId() == null || !user.getId().equals(existingUser.getId())) {
                throw new IllegalArgumentException("そのメールアドレスは既に登録されています: " + email);
            }
        }

        // --- 以下、既存の保存処理 ---
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
