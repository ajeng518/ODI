import type { SVGProps } from 'react';
const SvgSearch = (props: SVGProps<SVGSVGElement>) => (
  <svg
    width='16'
    height='16'
    viewBox='0 0 16 16'
    fill='none'
    xmlns='http://www.w3.org/2000/svg'
    {...props}>
    <path
      fillRule='evenodd'
      clipRule='evenodd'
      d='M1 7.14554C0.999557 4.21437 3.06975 1.69108 5.94452 1.11883C8.81929 0.546586 11.698 2.08477 12.82 4.79267C13.9421 7.50057 12.995 10.624 10.558 12.2528C8.12104 13.8815 4.87287 13.562 2.8 11.4895C1.64763 10.3376 1.00014 8.77495 1 7.14554Z'
      stroke='white'
      strokeWidth='1.5'
      strokeLinecap='round'
      strokeLinejoin='round'
    />
    <path
      d='M11.489 11.4905L15 15.0015'
      stroke='white'
      strokeWidth='1.5'
      strokeLinecap='round'
      strokeLinejoin='round'
    />
  </svg>
);
export default SvgSearch;
