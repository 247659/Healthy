export interface MedicalStaff {
    id: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    licenseNumber: string;
    specializations: Specialization[];
}

export interface Specialization {
    name: string;
    obtainedDate: string;
    certificateNumber: string;
}
