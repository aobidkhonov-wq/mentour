package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.enums.SchoolBookStatus;
import uz.tune.mentourBiz.rest.payload.res.ResBookCoinStats;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepository;
import uz.tune.mentourBiz.rest.service.school.SchoolService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.BOOK)
@RequiredArgsConstructor
public class BookEndpoint {

    private final SchoolBookRepository schoolBookRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolService schoolService;

    @GetMapping(BaseURI.ALL)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_CREATE)")
    public ResponseEntity<List<ResBooks>> getBooks() {
        return ResponseEntity.ok(schoolBookRepository.findAllByStatus(SchoolBookStatus.ACTIVE).stream().map(ResBooks::new).toList());
    }

    @GetMapping("/coin-stats/{bookUuid}")
    public ResponseEntity<List<ResBookCoinStats>> getBookCoinStats(@PathVariable(required = false) UUID bookUuid) {
        return ResponseEntity.ok(schoolService.getBookCoinStats(bookUuid));
    }

    @GetMapping(BaseURI.ALL + BaseURI.SCHOOLS + "/{schoolUuid}")
    public ResponseEntity<List<ResBooks>> getBooksForSchool(@PathVariable UUID schoolUuid) {
        List<SchoolBook> schoolBooks = schoolBookRepository.findAvailableForSchool(schoolUuid);

        return ResponseEntity.ok(schoolBooks.stream()
                .filter(b -> b.getStatus() == SchoolBookStatus.ACTIVE)
                .map(ResBooks::new)
                .toList());
    }

}
