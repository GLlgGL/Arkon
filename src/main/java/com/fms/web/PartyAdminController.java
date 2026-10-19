package com.fms.web;

import com.fms.domain.Client;
import com.fms.domain.Company;
import com.fms.domain.Merchant;
import com.fms.domain.NotFoundException;
import com.fms.domain.Party;
import com.fms.repo.ClientRepository;
import com.fms.repo.CompanyRepository;
import com.fms.repo.MerchantRepository;
import com.fms.repo.PartyRepository;
import jakarta.validation.Valid;
import java.util.function.Supplier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MerchantRegistration, CompanyRegistration and ClientRegistration.
 * These pages were not among the uploaded files; they are rebuilt from the
 * columns the rest of the code reads (Name, TIN, Address, Mail, Number).
 */
@Controller
public class PartyAdminController {

    /** The three registers that share this screen. */
    public enum Kind {
        merchants("Merchant", "Merchants"),
        companies("Company", "Companies"),
        clients("Client", "Clients");

        final String label;
        final String plural;

        Kind(String label, String plural) {
            this.label = label;
            this.plural = plural;
        }

        public String getPath() { return name(); }
        public String getLabel() { return label; }
        public String getPlural() { return plural; }
    }

    private final MerchantRepository merchants;
    private final CompanyRepository companies;
    private final ClientRepository clients;

    private final com.fms.service.AuditService audit;

    public PartyAdminController(MerchantRepository merchants, CompanyRepository companies, ClientRepository clients,
                                com.fms.service.AuditService audit) {
        this.audit = audit;
        this.merchants = merchants;
        this.companies = companies;
        this.clients = clients;
    }

    @SuppressWarnings("unchecked")
    private <T extends Party> PartyRepository<T> repo(Kind k) {
        return (PartyRepository<T>) switch (k) {
            case merchants -> merchants;
            case companies -> companies;
            case clients -> clients;
        };
    }

    private Supplier<? extends Party> factory(Kind k) {
        return switch (k) {
            case merchants -> Merchant::new;
            case companies -> Company::new;
            case clients -> Client::new;
        };
    }

    private static Kind kind(String path) {
        try {
            return Kind.valueOf(path);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Unknown register: " + path);
        }
    }

    @GetMapping("/admin/{kind:merchants|companies|clients}")
    public String list(@PathVariable String kind, Model model) {
        Kind k = kind(kind);
        model.addAttribute("kind", k);
        model.addAttribute("items", repo(k).findAllByOrderByNameAsc());
        return "admin/parties";
    }

    @GetMapping("/admin/{kind:merchants|companies|clients}/new")
    public String newForm(@PathVariable String kind, Model model) {
        model.addAttribute("kind", kind(kind));
        model.addAttribute("form", new PartyForm());
        model.addAttribute("itemId", null);
        return "admin/party-form";
    }

    @GetMapping("/admin/{kind:merchants|companies|clients}/{id:\\d+}")
    public String editForm(@PathVariable String kind, @PathVariable Long id, Model model) {
        Kind k = kind(kind);
        Party p = repo(k).findById(id).orElseThrow(() -> new NotFoundException(k.label + " not found"));
        model.addAttribute("kind", k);
        model.addAttribute("form", PartyForm.of(p));
        model.addAttribute("itemId", id);
        return "admin/party-form";
    }

    @PostMapping({"/admin/{kind:merchants|companies|clients}/new", "/admin/{kind:merchants|companies|clients}/{id:\\d+}"})
    public String save(@PathVariable String kind, @PathVariable(required = false) Long id,
                       @Valid @ModelAttribute("form") PartyForm form, BindingResult errors,
                       Model model, RedirectAttributes redirect) {
        Kind k = kind(kind);
        PartyRepository<Party> r = repo(k);
        boolean duplicate = id == null ? r.existsByName(form.getName()) : r.existsByNameAndIdNot(form.getName(), id);
        if (duplicate) {
            errors.rejectValue("name", "duplicate", "A " + k.label.toLowerCase() + " with this name already exists.");
        }
        if (errors.hasErrors()) {
            model.addAttribute("kind", k);
            model.addAttribute("itemId", id);
            return "admin/party-form";
        }
        Party p = id == null ? factory(k).get()
                : r.findById(id).orElseThrow(() -> new NotFoundException(k.label + " not found"));
        form.applyTo(p);
        r.save(p);
        audit.log(id == null ? "CREATE" : "UPDATE", k.label, p.getName(), null);
        redirect.addFlashAttribute("message", k.label + " saved.");
        return "redirect:/admin/" + k.getPath();
    }

    @PostMapping("/admin/{kind:merchants|companies|clients}/{id:\\d+}/delete")
    public String delete(@PathVariable String kind, @PathVariable Long id, RedirectAttributes redirect) {
        Kind k = kind(kind);
        repo(k).findById(id).ifPresent(p -> audit.log("DELETE", k.label, ((com.fms.domain.Party) p).getName(), null));
        repo(k).deleteById(id);
        redirect.addFlashAttribute("message", k.label + " deleted. Existing orders keep the name they were saved with.");
        return "redirect:/admin/" + k.getPath();
    }
}
