package eu.erasmuswithoutpaper.registry.manifestoverview;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManifestsNotifiedAboutDuplicatesRepository
    extends CrudRepository<ManifestAlreadyNotifiedAboutDuplicates, String> {
}
