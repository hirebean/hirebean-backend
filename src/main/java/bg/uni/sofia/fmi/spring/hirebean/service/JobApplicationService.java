package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.JobApplicationRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.JobApplicationResponse;
import bg.uni.sofia.fmi.spring.hirebean.model.enums.ApplicationStatus;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface JobApplicationService {

    JobApplicationResponse apply(Long candidateId, JobApplicationRequest request, MultipartFile cvFile);

    List<JobApplicationResponse> getApplicationsForCandidate(Long candidateId);

    List<JobApplicationResponse> getApplicationsForJobOffer(Long jobOfferId);

    JobApplicationResponse updateStatus(Long applicationId, ApplicationStatus status);
}
