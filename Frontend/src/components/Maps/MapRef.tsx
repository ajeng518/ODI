import { useEffect, useRef, useState } from 'react';
import LatLngAddStore from '@/stores/useLatLngAddStore';

const MapRef = () => {
  const ref = useRef<HTMLDivElement>(null);
  const [googleMap, setGoogleMap] = useState<google.maps.Map>();

  const { currentLat, currentLng } = LatLngAddStore();

  useEffect(() => {
    if (ref.current) {
      const initialMap = new window.google.maps.Map(ref.current, {
        center: {
          lat: currentLat,
          lng: currentLng,
        },
        zoom: 16,
      });

      setGoogleMap(initialMap);
    }
  }, []);

  return <div ref={ref} id='map' className='w-full h-lvh' />;
};

export default MapRef;