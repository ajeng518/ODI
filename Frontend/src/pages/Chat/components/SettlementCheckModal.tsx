import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { IChatInfo } from '@/types/Chat';
import axios from 'axios';
import { ViteConfig } from '@/apis/ViteConfig';
import { getCookie } from '@/utils/CookieUtil';
import { IEvaluation } from '@/types/Chat';
import { toast } from 'react-toastify';
import EvaluationForm from './EvaluationForm';
import StatusBar from './StatusBar';

interface SettlementCheckModalProps {
  info?: IChatInfo;
  paidAmount: number | null;
  settleAmount: number | null;
  onClose: () => void;
  chargeFee: () => void;
}

const SettlementCheckModal: React.FC<SettlementCheckModalProps> = ({
  info,
  paidAmount,
  settleAmount,
  onClose,
  chargeFee,
}) => {
  const navigate = useNavigate();

  const handleModalClick = (e: React.MouseEvent<HTMLDivElement>) => {
    e.stopPropagation(); // 모달 내부 클릭 이벤트 처리
  };

  const handleChargeAndClose = () => {
    chargeFee();
    onClose();
    // MUST : 동승자 평가 모달 open
    evalOpen();
  };
  const settle = settleAmount && paidAmount ? settleAmount - paidAmount : 0;

  /**
   * @description 동승자 평가 여부 및 평가 모달
   */

  const evalModalRef = useRef<HTMLDialogElement>(null);
  const [isEval, setIsEval] = useState(false);

  const evalOpen = () => {
    if (evalModalRef.current) {
      evalModalRef.current.showModal();
    }
  };

  const handleEvaluation = () => {
    setIsEval(true);
  };

  const closeModal = () => {
    setIsEval(false);
    if (evalModalRef.current) {
      evalModalRef.current.close();
    }
  };

  // NOTE : 나를 제외한 평가 인원
  const participants = [info!.organizer, ...info!.participants].filter(
    user => user.id !== info!.me.id,
  );

  const [currentStep, setCurrentStep] = useState<number>(0);
  const [reviews, setReviews] = useState<IEvaluation[]>(
    participants.map(user => ({
      reviewee_id: user.id,
      kindScore: 1,
      promiseScore: 1,
      fastChatScore: 1,
    })),
  );

  const handleNext = (index: number, scores: Partial<IEvaluation>) => {
    const newReviews = [...reviews];
    newReviews[index] = { ...newReviews[index], ...scores };
    setReviews(newReviews);
    setCurrentStep(currentStep + 1);
  };

  const handleSubmit = async () => {
    console.log('평가 대상 PARTY ID', info!.partyId);
    console.log('평가 내용', reviews);
    try {
      await axios.post(
        `${ViteConfig.VITE_BASE_URL}/api/members/brix`,
        {
          partyId: info!.partyId,
          memberBrixDTOList: reviews,
        },
        {
          headers: {
            AUTHORIZATION: getCookie('Authorization'),
          },
        },
      );
      toast.success('평가가 완료되었습니다.');
    } catch (error) {
      console.error(error);
      toast.error('평가 전송에 실패했습니다.');
    }
  };

  let evalModal = (
    <>
      <dialog ref={evalModalRef} id='my_modal_4' className='modal'>
        <div className='modal-box w-11/12'>
          {isEval === true ? (
            <>
              <h3 className='font-bold text-[20px]'>동승자 평가</h3>
              <div className='mt-1 border border-gray-300'></div>
              <div className='flex flex-col justify-between'>
                <StatusBar currentStep={currentStep} totalSteps={participants.length} />
                {currentStep < participants.length ? (
                  <EvaluationForm
                    person={participants[currentStep]}
                    onNext={scores => handleNext(currentStep, scores)}
                  />
                ) : (
                  <button onClick={handleSubmit} className='btn bg-green-500 text-white mt-4'>
                    평가 완료
                  </button>
                )}
              </div>
              <div className='modal-action'>
                <button
                  className='btn btn-sm btn-circle btn-ghost absolute right-5 top-5'
                  onClick={closeModal}
                  type='button'>
                  ✕
                </button>
              </div>
            </>
          ) : (
            <>
              <h3 className='font-bold text-[20px]'>동승자 평가</h3>
              <div className='mt-1 border border-gray-300'></div>
              <div className='flex flex-col justify-between'>
                <h4 className='mt-5 text-lg text-black'>평가 진행 상황 </h4>
                <h4 className='mt-5 text-lg text-gray-500'>
                  동승자 평가를 통해 상대방을 평가해 주세요.
                </h4>
              </div>
              <div className='modal-action'>
                <button
                  className='btn btn-sm btn-circle btn-ghost absolute right-5 top-5'
                  onClick={closeModal}
                  type='button'>
                  ✕
                </button>
                <button
                  className='btn bg-green-500 text-white'
                  type='button'
                  onClick={handleEvaluation}>
                  평가하기
                </button>
              </div>
            </>
          )}
        </div>
      </dialog>
    </>
  );

  return (
    <>
      {/* MUST : 동승자 평가 여부 모달 & 동승자 평가 모달 & 엔드포인트 POST */}
      {evalModal}
      <div className='modal modal-open' onClick={onClose}>
        <div role='alert' className='alert shadow-lg ' onClick={handleModalClick}>
          <button className='btn btn-sm btn-circle absolute right-4 top-4' onClick={onClose}>
            ✕
          </button>
          <svg
            xmlns='http://www.w3.org/2000/svg'
            fill='none'
            viewBox='0 0 24 24'
            className='stroke-current shrink-0 w-6 h-6'>
            <path
              strokeLinecap='round'
              strokeLinejoin='round'
              strokeWidth='2'
              d='M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'></path>
          </svg>
          <div>
            <h3 className='text-md'>선불 금액 : {paidAmount}</h3>
            <div className='font-bold text-xl'>
              {settle >= 0 ? (
                <p>추가로 내야할 금액 : {settle}</p>
              ) : (
                <p>돌려 받을 금액 : {settle}</p>
              )}
            </div>
          </div>
          <button onClick={handleChargeAndClose} className='btn btn-sm'>
            정산하기
          </button>
        </div>
      </div>
    </>
  );
};

export default SettlementCheckModal;
