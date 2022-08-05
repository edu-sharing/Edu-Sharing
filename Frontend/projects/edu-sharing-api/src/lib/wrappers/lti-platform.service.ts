import { LtiPlatformV13Service, NodeV1Service, SearchV1Service } from '../api/services';
import { Observable } from 'rxjs';
import { Tools } from '../api/models/tools';

export class LtiPlatformService {
    constructor(private ltiPlatformService: LtiPlatformV13Service) {}

    getTools(): Observable<Tools> {
        return this.ltiPlatformService.tools();
    }
}
