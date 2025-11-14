package one.pkg.tinyutils.minecraft.version.constraint;

import one.pkg.tinyutils.minecraft.version.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record OrConstraint(List<VersionConstraint> constraints, String original) implements VersionConstraint {
    public OrConstraint(List<VersionConstraint> constraints, String original) {
        this.constraints = new ArrayList<>(constraints);
        this.original = original;
    }

    @Override
    public boolean satisfies(Version version) {
        return constraints.stream().anyMatch(constraint -> constraint.satisfies(version));
    }

    @Override
    public List<String> getVersions() {
        return constraints.stream()
                .flatMap(constraint -> constraint.getVersions().stream())
                .collect(Collectors.toList());
    }

    @Override
    public String getLowVersion() {
        return constraints.stream()
                .map(VersionConstraint::getLowVersion)
                .filter(s -> !s.isEmpty())
                .map(Version::new)
                .min(Version::compareTo)
                .map(Version::toString)
                .orElse("");
    }

    @Override
    public String getMaxVersion() {
        return constraints.stream()
                .map(VersionConstraint::getMaxVersion)
                .filter(s -> !s.isEmpty())
                .map(Version::new)
                .max(Version::compareTo)
                .map(Version::toString)
                .orElse("");
    }
}