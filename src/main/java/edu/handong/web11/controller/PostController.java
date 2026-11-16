package edu.handong.web11.controller;

import edu.handong.web11.domain.Post;
import edu.handong.web11.dto.PostForm;
import edu.handong.web11.service.PostService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class PostController {
    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/posts";
    }

    @GetMapping("/posts")
    public String list(Model model) {
        model.addAttribute("posts", posts.all());
        return "posts/list";
    }

    @GetMapping("/posts/new")
    public String form(Model model) {
        model.addAttribute("postForm", new PostForm());
        return "posts/form";
    }

    // Line-By-Line 주석
    @PostMapping("/posts")
    public String create(@Valid @ModelAttribute("postForm") PostForm form, BindingResult errors,
                         @RequestParam(name = "files", required = false) List<MultipartFile> files) {
        // @Validdhk @ModelAttribute를 통해 제목과 내용을 postForm에 담고 DTO에 설정된 검증 조건을 확인한다.
        // BindingResult errors를 통해서 위에서 검사한 입력값의 오류 여부를 전달받는다.
        // @RequestParam을 통해서 이름이 files인 입력 요소에서 전송한 첨부파일들을 받는다. 그리고 첨부파일 없이 글을 작성 할 수도 있으므오 필수값으로 설정X
        if (errors.hasErrors()) return "posts/form"; // 입력값에 오류가 있으면 저장하지 않고 다시 작성화면으로 돌아간다.
        Long id = posts.create(form, files == null ? List.of() : files); // Service에 게시글과 첨부파일 저장을 요청하고 파일 목록이 null이면 빈 목록으로 바꿔서 전달한다.
        return "redirect:/posts/" + id; // 저장된 계시글의 상세 주소로 다시 요청하도록 브라우저를 이동시킨다.
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("post", posts.get(id));
        return "posts/detail";
    }

    @GetMapping("/posts/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Post post = posts.get(id);
        PostForm form = new PostForm();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        model.addAttribute("postForm", form);
        model.addAttribute("postId", id);
        return "posts/form";
    }

    @PostMapping("/posts/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("postForm") PostForm form, BindingResult errors,
                         Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("postId", id);
            return "posts/form";
        }
        posts.update(id, form);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id) {
        posts.delete(id);
        return "redirect:/posts";
    }
}
