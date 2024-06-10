import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'propertySlug',
})
export class PropertySlugPipe implements PipeTransform {
    transform(value: string): string {
        return value?.replace(/[:.]/, '_');
    }
}
