package bootiful.api;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@ResponseBody
class MeHttpController {

    @GetMapping("/me")
    Map<String, String> principal() {
        var principal = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication().getName();
        return Map.of("name", principal);
    }
}
