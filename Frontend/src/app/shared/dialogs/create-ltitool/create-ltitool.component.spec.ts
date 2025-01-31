import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateLtitoolComponent } from './create-ltitool.component';
import { RestNodeService } from '../../../core-module/rest/services/rest-node.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { LtiPlatformService } from 'ngx-edu-sharing-api';

class MockRestNodeService {}
class MockRestNodeHelperService {}
class MockLtiPlatformService {}
describe('CreateLtitoolComponent', () => {
    let component: CreateLtitoolComponent;
    let fixture: ComponentFixture<CreateLtitoolComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            providers: [
                { provide: RestNodeService, useClass: MockRestNodeService },
                { provide: NodeHelperService, useClass: MockRestNodeHelperService },
                { provide: LtiPlatformService, useClass: MockLtiPlatformService },
            ],
            declarations: [CreateLtitoolComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(CreateLtitoolComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        void expect(component).toBeTruthy();
    });
});
