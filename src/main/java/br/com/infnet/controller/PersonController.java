package br.com.infnet.controller;

import br.com.infnet.model.Person;
import br.com.infnet.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Controller que gera HTML diretamente (strings).
 * Útil para iniciantes, sem necessidade de templates externos.
 */
@Controller
public class PersonController {

    private final PersonRepository repo;

    // Injeção de dependência via construtor (recomendado)
    @Autowired
    public PersonController(PersonRepository repo) {
        this.repo = repo;
    }

    // Home -> redireciona para /persons
    @GetMapping("/")
    public String home() {
        return "redirect:/persons";
    }

    // Listagem de pessoas
    @GetMapping(value = "/persons", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String listPersons(@RequestParam(value = "msg", required = false) String msg) {
        List<Person> all = repo.findAll();
        StringBuilder html = new StringBuilder();
        html.append(pageHeader("Lista de Pessoas"));
        if (msg != null) {
            html.append("<div class=\"alert alert-info\" role=\"alert\">").append(escape(msg)).append("</div>");
        }
        html.append("<div class=\"mb-3\">")
                .append("<a class=\"btn btn-primary\" href=\"/person/create\">Novo Cadastro</a>")
                .append("</div>");
        html.append("<table class=\"table table-striped table-responsive\">")
                .append("<thead><tr><th>ID</th><th>Nome</th><th>Email</th><th>Telefone</th><th>Ações</th></tr></thead>")
                .append("<tbody>");
        for (Person p : all) {
            html.append("<tr>")
                    .append("<td>").append(p.getId()).append("</td>")
                    .append("<td>").append(escape(p.getName())).append("</td>")
                    .append("<td>").append(escape(p.getEmail())).append("</td>")
                    .append("<td>").append(escape(p.getPhone())).append("</td>")
                    .append("<td>")
                    .append("<a class=\"btn btn-sm btn-success me-1\" href=\"/person/edit/").append(p.getId()).append("\">Editar</a>")
                    .append("<form style=\"display:inline;\" method=\"post\" action=\"/person/delete/").append(p.getId()).append("\" onsubmit=\"return confirm('Confirmar exclusão?');\">")
                    .append("<button class=\"btn btn-sm btn-danger\" type=\"submit\">Excluir</button>")
                    .append("</form>")
                    .append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        html.append(pageFooter());
        return html.toString();
    }

    // Formulário de criação
    @GetMapping(value = "/person/create", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String createForm(@RequestParam(value = "error", required = false) String error) {
        return renderForm(new Person(), "/person/create", "Criar Pessoa", error);
    }

    // Recebe POST de criação
    @PostMapping(value = "/person/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String handleCreate(@RequestParam String name, @RequestParam String email, @RequestParam String phone) {
        try {
            validateInput(name, email);
            Person p = new Person(null, name, email, phone);
            repo.save(p);
            return "redirect:/persons?msg=" + urlEncode("Pessoa criada com sucesso (ID=" + p.getId() + ")");
        } catch (IllegalArgumentException ex) {
            return "redirect:/person/create?error=" + urlEncode(ex.getMessage());
        } catch (Exception e) {
            return "redirect:/person/create?error=" + urlEncode("Erro inesperado: " + e.getMessage());
        }
    }

    // Formulário de edição
    @GetMapping(value = "/person/edit/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String editForm(@PathVariable Long id, @RequestParam(value = "error", required = false) String error) {
        Optional<Person> opt = repo.findById(id);
        if (opt.isEmpty()) {
            return listPersons("Pessoa não encontrada: ID=" + id);
        }
        return renderForm(opt.get(), "/person/edit/" + id, "Editar Pessoa (ID=" + id + ")", error);
    }

    // Recebe POST de edição
    @PostMapping(value = "/person/edit/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String handleEdit(@PathVariable Long id, @RequestParam String name, @RequestParam String email, @RequestParam String phone) {
        try {
            validateInput(name, email);
            Optional<Person> existing = repo.findById(id);
            if (existing.isEmpty()) {
                return "redirect:/persons?msg=" + urlEncode("Pessoa não encontrada: ID=" + id);
            }
            Person p = existing.get();
            p.setName(name);
            p.setEmail(email);
            p.setPhone(phone);
            repo.save(p);
            return "redirect:/persons?msg=" + urlEncode("Pessoa atualizada com sucesso (ID=" + id + ")");
        } catch (IllegalArgumentException ex) {
            return "redirect:/person/edit/" + id + "?error=" + urlEncode(ex.getMessage());
        } catch (Exception e) {
            return "redirect:/person/edit/" + id + "?error=" + urlEncode("Erro inesperado: " + e.getMessage());
        }
    }

    // Delete
    @PostMapping("/person/delete/{id}")
    public String handleDelete(@PathVariable Long id) {
        try {
            boolean removed = repo.delete(id);
            if (removed) {
                return "redirect:/persons?msg=" + urlEncode("Pessoa excluída (ID=" + id + ")");
            } else {
                return "redirect:/persons?msg=" + urlEncode("Pessoa não encontrada: ID=" + id);
            }
        } catch (Exception e) {
            return "redirect:/persons?msg=" + urlEncode("Erro ao excluir: " + e.getMessage());
        }
    }

    // ----- Helpers -----
    private String renderForm(Person p, String action, String title, String error) {
        StringBuilder html = new StringBuilder();
        html.append(pageHeader(title));
        if (error != null) {
            html.append("<div class=\"alert alert-danger\" role=\"alert\">").append(escape(error)).append("</div>");
        }
        html.append("<form method=\"post\" action=\"").append(action).append("\">")
                .append("<div class=\"mb-3\"><label class=\"form-label\">Nome</label>")
                .append("<input class=\"form-control\" name=\"name\" value=\"").append(escapeOrEmpty(p.getName())).append("\" required/></div>")
                .append("<div class=\"mb-3\"><label class=\"form-label\">Email</label>")
                .append("<input class=\"form-control\" name=\"email\" value=\"").append(escapeOrEmpty(p.getEmail())).append("\" type=\"email\" required/></div>")
                .append("<div class=\"mb-3\"><label class=\"form-label\">Telefone</label>")
                .append("<input class=\"form-control\" name=\"phone\" value=\"").append(escapeOrEmpty(p.getPhone())).append("\"/></div>")
                .append("<button class=\"btn btn-primary\" type=\"submit\">Salvar</button> ")
                .append("<a class=\"btn btn-secondary\" href=\"/persons\">Voltar</a>")
                .append("</form>");
        html.append(pageFooter());
        return html.toString();
    }

    private void validateInput(String name, String email) {
        if (isEmpty(name) || name.length() < 3) {
            throw new IllegalArgumentException("Nome deve ter ao menos 3 caracteres.");
        }
        if (isEmpty(email) || !email.contains("@") || email.length() < 5) {
            throw new IllegalArgumentException("Email inválido.");
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String pageHeader(String title) {
        return "<!doctype html>\n" +
                "<html lang=\"pt-br\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "  <title>" + escape(title) + "</title>\n" +
                "  <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"container mt-4\">\n" +
                "<h1>" + escape(title) + "</h1>\n" +
                "<hr/>";
    }

    private String pageFooter() {
        return "</div>\n" +
                "<script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js\"></script>\n" +
                "</body>\n" +
                "</html>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeOrEmpty(String s) {
        return s == null ? "" : escape(s);
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao codificar a string", e);
        }
    }

    @GetMapping("/simulate/error")
    public String simulateError() {
        throw new IllegalStateException("Erro simulado pelo usuário");
    }

    @GetMapping("/simulate/timeout")
    public String simulateTimeout() throws InterruptedException {
        Thread.sleep(5000);
        return "redirect:/persons";
    }

    @PostMapping("/simulate/failfast")
    public String simulateFailFast(@RequestParam String input) {
        if (input.contains("<script>") || input.contains("DROP TABLE")) {
            throw new IllegalArgumentException("Entrada maliciosa detectada");
        }
        return "redirect:/persons";
    }

}