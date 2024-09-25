package org.edu_sharing.repository.server.rendering;

import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.ErrorFilter;
import org.edu_sharing.repository.server.tools.HttpException;
import org.edu_sharing.service.InsufficientPermissionException;
import org.json.JSONObject;

public class RenderingException extends ErrorFilter.ErrorFilterException {

    public static RenderingException fromThrowable(Throwable throwable) {
        if(throwable instanceof InsufficientPermissionException || throwable instanceof AccessDeniedException)   {
            I18N i18nKey = I18N.permissions_missing;

            if(throwable instanceof AccessDeniedException && CCConstants.PERMISSION_EMBED.equals(((AccessDeniedException) throwable).getMsgId())){
                i18nKey = I18N.permissions_embed_missing;
            }

            return new RenderingException(
                    HttpServletResponse.SC_FORBIDDEN,
                    throwable.getMessage(),
                    i18nKey,
                    throwable
            );
        }
        return new RenderingException(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                throwable.getMessage(),
                RenderingException.I18N.unknown,
                throwable
        );
    }

    public enum I18N{
        invalid_parameters,
        encryption,
        usage_missing,
        node_missing,
        usage_missing_permissions,
        permissions_missing,
        permissions_embed_missing,
        internal,
        unknown,
    }
    private String technicalDetail;
    private I18N i18nName;
    private Throwable nested;

    public RenderingException(int statusCode,String technicalDetail, I18N i18nName) {
        super(statusCode);
        this.technicalDetail=technicalDetail;
        this.i18nName=i18nName;
    }
    public RenderingException(HttpException exception){
        super(exception.getStatusCode());
        this.nested=exception;
        try{
            JSONObject json = new JSONObject(exception.getMessage());
            this.i18nName=I18N.valueOf(json.getString("i18nName"));
            this.technicalDetail=json.getString("technicalDetail");
        }
        catch(Throwable t){
            this.technicalDetail=exception.getMessage();
            this.i18nName=I18N.unknown;
        }
    }
    public RenderingException(int statusCode,String technicalDetail, I18N i18nName,Throwable nested) {
        this(statusCode,technicalDetail,i18nName);
        this.nested=nested;

    }

    public String getTechnicalDetail() {
        return technicalDetail;
    }

    public String getI18nName() {
        return i18nName.name();
    }

    public Throwable getNested() {
        return nested;
    }
}
