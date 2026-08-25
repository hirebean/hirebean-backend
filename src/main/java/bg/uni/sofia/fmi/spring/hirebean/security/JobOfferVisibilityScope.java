package bg.uni.sofia.fmi.spring.hirebean.security;

public record JobOfferVisibilityScope(boolean allStatuses, Long managedCompanyId) {

    public static JobOfferVisibilityScope fullVisibility() {
        return new JobOfferVisibilityScope(true, null);
    }

    public static JobOfferVisibilityScope managedCompanyVisibility(Long companyId) {
        return new JobOfferVisibilityScope(false, companyId);
    }

    public static JobOfferVisibilityScope publicVisibility() {
        return new JobOfferVisibilityScope(false, null);
    }
}
