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
import com.example.demo.service.UserRegistService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MailUserController {

	private final VirtualUserRepository userRepository;
	private final VirtualDomainRepository domainRepository;
	private final InputCsvService csvService;
	private final UserRegistService registService;
	
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
    public String register(@ModelAttribute UserForm form,Model model,RedirectAttributes redirectAttributes) {
    	try {
            registService.saveUserFromForm(form, new VirtualUser()); 
        } catch (IllegalArgumentException e) {
            // ★エラーが発生した場合
            model.addAttribute("error", e.getMessage()); // エラーメッセージをセット
            model.addAttribute("domains", domainRepository.findAll());
            return "user_add"; // 入力画面（新規登録）に戻る
        }
        return "redirect:/user/list"; 
    }
    

    // --- ユーザ編集 ---

    // 7. 編集フォーム表示
    @GetMapping("/user/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model,RedirectAttributes redirectAttributes) {
        VirtualUser user = userRepository.findById(id).orElse(null);
        
     // ★追加: ユーザが存在しない場合の処理
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "指定されたユーザ(ID:" + id + ")は見つかりませんでした");
            return "redirect:/user/list";
        }
        
        
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
    public String update(@RequestParam Integer id, @ModelAttribute UserForm form,Model model) {
    	try {
            VirtualUser user = userRepository.findById(id).orElseThrow();
            registService.saveUserFromForm(form, user);
        } catch (IllegalArgumentException e) {
            // ★エラーが発生した場合
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("domains", domainRepository.findAll());
            return "user_edit"; // 入力画面（編集）に戻る
        }
        return "redirect:/user/list";
    }
    
    @PostMapping("/user/import")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
    	if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ファイルが選択されていません");
            return "redirect:/user/list";
        }

        // Serviceから結果オブジェクトを受け取る
        InputCsvService.ImportResult result = csvService.importCsvUsers(file);
        
        if (result.isHasError()) {
            // エラーがあった場合、サマリーと詳細リストの両方を渡す
            redirectAttributes.addFlashAttribute("error", result.getSummaryMessage());
            redirectAttributes.addFlashAttribute("errorList", result.getErrorDetails());
        } else {
            // 成功のみの場合
            redirectAttributes.addFlashAttribute("msg", result.getSummaryMessage());
        }

        return "redirect:/user/list";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}