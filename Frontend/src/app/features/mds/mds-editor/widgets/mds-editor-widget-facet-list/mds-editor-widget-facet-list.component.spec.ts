import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { TestBed } from '@angular/core/testing';
import { MdsEditorWidgetFacetListComponent } from './mds-editor-widget-facet-list.component';
import { TranslateService } from '@ngx-translate/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ChangeDetectorRef } from '@angular/core';
import { EduSharingApiModule, FacetValue } from 'ngx-edu-sharing-api';

describe('MdsEditorWidgetFacetList', () => {
    let component: MdsEditorWidgetFacetListComponent;
    let mdsEditorInstanceServiceStub: MdsEditorInstanceService;
    let translateServiceStub: TranslateService;
    let changeDetectorRefStuf: ChangeDetectorRef;

    beforeEach(() => {
        mdsEditorInstanceServiceStub = {} as unknown as MdsEditorInstanceService;
    });

    function setUp() {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule, EduSharingApiModule.forRoot()],
            providers: [
                MdsEditorWidgetFacetListComponent,
                {
                    provide: MdsEditorInstanceService,
                    useValue: mdsEditorInstanceServiceStub,
                },
                {
                    provide: TranslateService,
                    useValue: translateServiceStub,
                },
                {
                    provide: ChangeDetectorRef,
                    useValue: changeDetectorRefStuf,
                },
            ],
        });
        component = TestBed.inject(MdsEditorWidgetFacetListComponent);
        (component.widget as Partial<Widget>) = {
            definition: {
                type: 'facetList',
            },
        };
    }

    it('should be created', () => {
        setUp();
        void expect(component).toBeTruthy();
    });

    it('Filter disabled', () => {
        setUp();
        expect(component.hasFilter()).toBeFalse();
        component.widget.definition.filterMode = 'disabled';
        expect(component.hasFilter()).toBeFalse();
        component.widget.definition.filterMode = 'auto';
        component.facetAggregationSubject.next({
            hasMore: false,
            values: new Array<FacetValue>(4),
        });
        expect(component.hasFilter()).toBeFalse();
    });
    it('Filter enabled', () => {
        setUp();
        component.widget.definition.filterMode = 'always';
        expect(component.hasFilter()).toBeTrue();
        component.widget.definition.filterMode = 'auto';
        component.facetAggregationSubject.next({
            hasMore: true,
            values: new Array<FacetValue>(4),
        });
        expect(component.hasFilter()).toBeTrue();
        component.facetAggregationSubject.next({
            hasMore: false,
            values: new Array<FacetValue>(6),
        });
        expect(component.hasFilter()).toBeTrue();
    });
});
