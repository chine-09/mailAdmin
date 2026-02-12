package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.example.demo.service.InputCsvService;
import com.example.demo.service.OutputCsvService;
import com.example.demo.service.UserService; // UserRegistServiceから変更

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MailUserController {
    
    private final UserService userService; // ★統合されたService
    private final InputCsvService csvService;
    private final OutputCsvService outputCsvService;
    
    // 1. トップメニュー
    @GetMapping("/")
    public String top() {
        return "top";
    }

    // 2. ユーザ一覧画面
    @GetMapping("/user/list")
    public String list(@RequestParam(required = false) String keyword, Model model) {
    	List<VirtualUser> users;

        if (keyword != null && !keyword.isEmpty()) {
            // キーワードがある場合：検索実行
            users = userService.searchUsers(keyword);
        } else {
            // キーワードがない場合：全件取得
            users = userService.findAllUsers();
        }

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword); // 画面の検索窓に文字を残すために渡す
        
        return "user_list";
    }

    // 3. ユーザ削除処理
    @PostMapping("/user/delete/{id}")
    public String delete(@PathVariable Integer id) {
        // RepositoryではなくServiceを使う
        userService.deleteUser(id);
        return "redirect:/user/list";
    }

    // --- ユーザ登録 (新規) ---

    // 4. 登録フォーム表示
    @GetMapping("/user/add")
    public String showAddForm(@ModelAttribute UserForm userForm, Model model) {
        // ドメイン一覧もServiceから取得
        model.addAttribute("domains", userService.findAllDomains());
        return "user_add";
    }

    // 5. 登録確認画面へ
    @PostMapping("/user/confirm")
    public String confirm(@ModelAttribute UserForm form, Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "パスワードが一致しません");
            model.addAttribute("domains", userService.findAllDomains()); // Serviceを使用
            return "user_add";
        }
        
        VirtualDomain domain = userService.findAllDomains().stream()
                .filter(d -> d.getId().equals(form.getDomainId()))
                .findFirst()
                .orElseThrow();
                
        model.addAttribute("domainName", domain.getName());
        model.addAttribute("emailPreview", form.getUsername() + "@" + domain.getName());
        
        return "confirm";
    }

    // 6. 登録実行
    @PostMapping("/user/register")
    public String register(@ModelAttribute UserForm form, Model model, RedirectAttributes redirectAttributes) {
        try {
            userService.saveUserFromForm(form, new VirtualUser()); 
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("domains", userService.findAllDomains()); // Serviceを使用
            return "user_add";
        }
        return "redirect:/user/list"; 
    }

    // --- ユーザ編集 ---

    // 7. 編集フォーム表示
    @GetMapping("/user/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Service経由で取得
            VirtualUser user = userService.findUserById(id);
            
            UserForm form = new UserForm();
            form.setDomainId(user.getVirtualDomain().getId());
            
            String fullEmail = user.getEmail();
            String username = fullEmail.substring(0, fullEmail.indexOf('@'));
            form.setUsername(username);
            
            model.addAttribute("userId", id);
            model.addAttribute("userForm", form);
            model.addAttribute("domains", userService.findAllDomains()); // Serviceを使用
            return "user_edit";
            
        } catch (IllegalArgumentException e) {
            // ユーザが見つからない場合
            redirectAttributes.addFlashAttribute("error", "指定されたユーザは見つかりませんでした");
            return "redirect:/user/list";
        }
    }

    // 8. 編集実行
    @PostMapping("/user/update")
    public String update(@RequestParam Integer id, @ModelAttribute UserForm form, Model model) {
        try {
            // Service経由で取得
            VirtualUser user = userService.findUserById(id);
            userService.saveUserFromForm(form, user);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("domains", userService.findAllDomains()); // Serviceを使用
            return "user_edit";
        }
        return "redirect:/user/list";
    }
    
    // CSVインポート
    @PostMapping("/user/import")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "ファイルが選択されていません");
            return "redirect:/user/list";
        }

        InputCsvService.ImportResult result = csvService.importCsvUsers(file);
        
        if (result.isHasError()) {
            redirectAttributes.addFlashAttribute("error", result.getSummaryMessage());
            redirectAttributes.addFlashAttribute("errorList", result.getErrorDetails());
        } else {
            redirectAttributes.addFlashAttribute("msg", result.getSummaryMessage());
        }

        return "redirect:/user/list";
    }
    
 // ★追加: 確認画面から「戻る」ボタンが押されたときの処理
    @PostMapping("/user/add/back")
    public String backToForm(@ModelAttribute UserForm form, Model model) {
        // ドロップダウンリストの再取得が必要
        model.addAttribute("domains", userService.findAllDomains());
        
        return "user_add";
    }
    
    @GetMapping("/user/export")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csvData = outputCsvService.generateUserListCsv();
        
        String fileName = "user_list.csv";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }  
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}