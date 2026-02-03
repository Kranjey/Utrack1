package tests.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class IssueRequest {
    private String projectId;
    private String summary;
    private String description;
    
    public IssueRequest(String projectId, String summary, String description) {
        this.projectId = projectId;
        this.summary = summary;
        this.description = description;
    }
}