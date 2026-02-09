package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.VirtualDomain;
import com.example.demo.entity.VirtualUser;
import com.example.demo.form.UserForm;
import com.example.demo.repository.VirtualDomainRepository;
import com.example.demo.repository.VirtualUserRepository;
import com.example.demo.service.InputCsvService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MailUserController {

	private final VirtualUserRepository userRepository;
	private final VirtualDomainRepository domainRepository;
	private final InputCsvService csvService;
	
	// 1. トップメニュー
    @GetMapping("/")
    public String top() {
        return "top";
    }


    // 2. ユーザ一覧画面
    @GetMapping("/user/list")
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "user_list";
    }

    // 3. ユーザ削除処理
    @PostMapping("/user/delete/{id}")
    public String delete(@PathVariable Integer id) {
        userRepository.deleteById(id);
        return "redirect:/user/list"; // 削除後は一覧に戻る
    }

    // --- ユーザ登録 (新規) ---

    // 4. 登録フォーム表示
    @GetMapping("/user/add")
    public String showAddForm(@ModelAttribute UserForm userForm, Model model) {
        //model.addAttribute("userForm", new UserForm());
        model.addAttribute("domains", domainRepository.findAll());
        return "user_add";
    }

    // 5. 登録確認画面へ
    @PostMapping("/user/confirm")
    public String confirm(@ModelAttribute UserForm form, Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "パスワードが一致しません");
            model.addAttribute("domains", domainRepository.findAll());
            return "user_add"; // エラーなら登録画面に戻る
        }

        VirtualDomain domain = domainRepository.findById(form.getDomainId()).orElseThrow();
        model.addAttribute("domainName", domain.getName());
        model.addAttribute("emailPreview", form.getUsername() + "@" + domain.getName());
        
        return "confirm";
    }

    // 6. 登録実行
    @PostMapping("/user/register")
    public String register(@ModelAttribute UserForm form) {
        saveUser(form, new VirtualUser()); // 新規作成
        return "redirect:/user/list"; // 完了後は一覧へ
    }

    // --- ユーザ編集 ---

    // 7. 編集フォーム表示
    @GetMapping("/user/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        VirtualUser user = userRepository.findById(id).orElseThrow();
        
        // 既存データをフォームに詰める作業
        UserForm form = new UserForm();
        form.setDomainId(user.getVirtualDomain().getId());
        
        // email (test@example.com) から user (test) 部分を取り出す
        String fullEmail = user.getEmail();
        String username = fullEmail.substring(0, fullEmail.indexOf('@'));
        form.setUsername(username);
        
        // ※パスワードはハッシュ化されているため、フォームには空で表示し、
        // 入力された場合のみ更新する運用にします。

        model.addAttribute("userId", id); // 更新時にIDが必要
        model.addAttribute("userForm", form);
        model.addAttribute("domains", domainRepository.findAll());
        return "user_edit";
    }

    // 8. 編集実行
    @PostMapping("/user/update")
    public String update(@RequestParam Integer id, @ModelAttribute UserForm form) {
        // IDから既存ユーザを取得して更新
        VirtualUser user = userRepository.findById(id).orElseThrow();
        saveUser(form, user);
        return "redirect:/user/list";
    }
    
    @PostMapping("/user/import")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ファイルが選択されていません");
            return "redirect:/user/list";
        }

        String resultMessage = csvService.importCsvUsers(file);
        
        // メッセージの種類によって表示色を変えたい場合の簡易判定（任意）
        if (resultMessage.startsWith("エラー")) {
            redirectAttributes.addFlashAttribute("error", resultMessage);
        } else {
            redirectAttributes.addFlashAttribute("msg", resultMessage);
        }

        return "redirect:/user/list";
        
        
        
        
    }

    // (共通処理) 保存ロジック
    private void saveUser(UserForm form, VirtualUser user) {
        VirtualDomain domain = domainRepository.findById(form.getDomainId()).orElseThrow();
        user.setVirtualDomain(domain);
        
        // パスワードが入力されている場合のみ更新（空なら変更しない）
        if (form.getPassword() != null && !form.getPassword().isEmpty()) {
            user.setPassword(form.getPassword());
        }

        String email = form.getUsername() + "@" + domain.getName();
        user.setEmail(email);

        String maildir = domain.getName() + "/" + form.getUsername() + "/";
        user.setMaildir(maildir);

        userRepository.save(user);
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}