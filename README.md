## Week 11 — Attachment Board 22200619 임성열

## Project Information
### 프로젝트 소개
### GitHub Repository URL
### Render Deployment URL

## Installation: 환경 설정 및 실행 방법
## Source Code Exploration
### Project Structure & Spring MVC
### 1. PostController, PostService, PostRepository는 각각 어떤 역할을 하며, 서로 어떻게 연결되는가?
- PostController는 사용자가 게시글을 작성하거나 조회할 때 보내는 요청을 받고 적절한 처리 결과 화면으로 연결한다. 
- PostService는 게시글 생성, 수정, 삭제와 같은 실제 작업을 처리하고 PostRepository는 JPA를 이용해 DB에 게시글을 저장하거나 조회한다. 
- 예를 들어 계시판 작성 요청이 들어오는 경우 PostController.create()에서 PostService.create()로 전달되고, 마지막에 Repository의 saveAndFlush()를 통해 DB에 저장되는 방식으로 서로 작동한다.
### 2. 게시글 작성 시 HTML Form의 입력값과 첨부파일은 Controller에 어떻게 전달되는가?
- 작성 화면의 Form은 POST 방식과 enctype="multipart/form-data"를 사용하여 글 내용과 파일을 함께 전송한다. 
- 제목과 내용은 @ModelAttribute를 통해 PostForm 객체에 담기고, 첨부파일은 @RequestParam(name = "files")를 통해 List<MultipartFile>로 전달된다. 

### JPA & Entity Relationship
### 1. Post와 Attachment Entity는 어떤 관계이며, @ManyToOne과 @OneToMany는 각각 어떤 역할을 하는가?
- 게시글 하나에 여러 첨부파일을 등록할 수 있으므로 Post와 Attachment는 일대다 관계이다. 
- Post의 @OneToMany는 한 게시글에 연결된 여러 첨부파일을 나타내고, Attachment의 @ManyToOne은 각 첨부파일이 하나의 게시글에 속한다는 것을 나타낸다.
### 2. cascade = CascadeType.ALL과 orphanRemoval = true는 어떤 역할을 하며, 실제 디스크에 저장된 파일에도 영향을 주는가?
- cascade = CascadeType.ALL은 부모의 생명주기를 자식에게 전달하는 역할이고, orphanRemoval = true는 부모와 연관관계가 끊어진 자식을 자동 삭제하는 역할을 한다. 
- 두 설정은 DB에 저장된 Entity를 관리하는 기능이므로 실제 디스크의 파일을 직접 삭제하지는 않고, 이 프로젝트에서는 PostService.delete()가 FileStorageService.deleteIfExists()를 호출하여 파일을 따로 삭제한다.

### File Upload
### 1. MultipartFile은 무엇이며, 업로드된 파일은 어떤 과정을 거쳐 서버에 저장되는가?
- MultipartFile은 사용자가 HTTP 요청으로 보낸 업로드 파일을 다루기 위한 Spring의 인터페이스이다. 
- FileStorageService.store()에서는 파일 이름, 확장자, 크기 등을 검사하고 UUID를 이용해 저장할 이름과 경로를 만든 뒤 Files.copy()로 실제 파일을 저장한다. 
- 저장 후에는 원본 파일명, 저장 파일명, 타입, 크기를 담은 Attachment 객체를 반환하고 PostService.create()에서 게시글과 함께 DB에 저장한다.
### 2. 파일을 원본 이름 대신 UUID 기반 파일명으로 저장하는 이유는 무엇인가?
- 서로 다른 사용자가 같은 이름의 파일을 업로드하면 저장 파일명이 겹칠 수 있다. 
- UUID를 사용하면 파일명 충돌 가능성을 매우 낮출 수 있어서 같은 원본 이름을 가진 파일도 구분하여 저장할 수 있다.

### File Download & Security
### 1. 파일 다운로드 시 Resource, ResponseEntity, Content-Disposition은 각각 어떤 역할을 하는가?
- Resource는 서버에 저장된 파일을 Spring에서 읽고 전달할 수 있도록 표현하는 객체이다. 
- ResponseEntity는 파일 데이터와 함께 HTTP 상태 코드, 헤더 등을 설정하여 응답을 만드는 역할을 한다. 
- Content-Disposition은 브라우저가 파일을 다운로드하도록 안내하고, 저장할 때 사용할 원본 파일명을 전달한다.

### 2. 파일 저장 과정에서 normalize()와 startsWith()를 사용하는 이유는 무엇인가?
- normalize()는 경로에 포함된 .이나 .. 같은 부분을 정리하여 경로를 비교하기 쉽게 만든다. 
- startsWith(uploadDir)는 정리한 저장 경로가 지정된 업로드 폴더 안에 있는지 확인한다. 
- 두 검사를 함께 사용하면 잘못된 경로를 이용해 업로드 폴더 밖에 파일을 저장하려는 시도를 막는 데 도움이 된다.

## Customizing: 수정 내용 3가지 및 변경 전/후 결과

### 1. Text: 게시판 제목과 버튼 문구 변경
- 목록 화면의 제목을 `성열의 자료 공유 게시판`으로 변경함
- 게시글 작성 버튼의 문구도 `+ 게시글 추가`로 변경했다. 문구만 변경했으며, 버튼을 누르면 기존과 동일하게 `/posts/new` 작성 화면으로 이동.

**변경 전: 샘플 초기 화면**
![Customizing 이전의 게시판 목록](results/01_sample.png)

**변경 후: 제목과 버튼 문구 수정**
![게시판 제목과 버튼 문구 변경 결과](results/custom_List.png)

### 2. Style: 버튼 색상 변경
- 버튼의 변화를 쉽게 확인할 수 있도록 기본 배경색과 테두리 색상을 초록색(`#198754`)으로 바꾸고 글자색을 흰색으로 지정. 
- 마우스를 올리거나 포커스·클릭 상태가 되면 진한 초록색(`#146c43`)이 적용.
![버튼 색상을 초록색으로 변경한 결과](results/custom_css.png)

### 3. UI/Data: 첨부파일 타입 표시

- 첨부파일의 종류를 쉽게 확인할 수 있도록 파일 이름 옆에 타입을 표시. 
- `th:each`로 첨부파일을 반복해서 보여 주는 부분에 `th:text="${file.contentType}"`를 사용하는 `span`을 추가하고, Bootstrap의 `badge bg-secondary ms-2` 클래스로 회색 배지 모양을 적용. 

![첨부파일 타입 배지를 추가한 상세 화면](results/custom_detail.png)

## New Feature: 이미지 미리보기 구현 내용
## Testing & Deployment: results/ 스크린샷 4장 이상 및 Render 배포 URL
## Weekly Review:
## 새롭게 배운 내용 3가지
## 어려웠던 점과 해결 방법
## AI 활용 내용 및 느낀 점
